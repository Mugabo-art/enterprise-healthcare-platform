import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getPatient } from '../../services/patientService.js';
import { listVisits, createVisit, updateVisit } from '../../services/visitService.js';
import { listPrescriptions, createPrescription } from '../../services/doctorService.js';
import { listHistory, createHistoryEntry } from '../../services/medicalHistoryService.js';
import { listAttachments, uploadAttachment, downloadAttachment } from '../../services/attachmentService.js';
import { listLabRequests, createLabRequest, updateLabRequestStatus, recordLabResult, getLabResult } from '../../services/labService.js';
import { useAuth } from '../../context/AuthContext.jsx';
import AppShell from '../Layout/AppShell.jsx';
import { STATUS_LABELS, VISIT_STATUSES } from '../Doctor/visitStatus.js';
import styles from './PatientDetailPage.module.css';

const VISIT_TYPES = ['OUTPATIENT', 'INPATIENT', 'EMERGENCY', 'FOLLOW_UP'];
const HISTORY_CATEGORIES = ['CONDITION', 'ALLERGY', 'MEDICATION', 'SURGERY', 'IMMUNIZATION', 'FAMILY_HISTORY'];
const CAN_WRITE_CLINICAL = ['ADMIN', 'NURSE', 'DOCTOR'];
const CAN_RECORD_LAB_RESULTS = ['ADMIN', 'LAB_TECH'];
const LAB_STATUS_LABELS = { REQUESTED: 'Requested', IN_PROGRESS: 'In progress', COMPLETED: 'Completed', CANCELLED: 'Cancelled' };

export default function PatientDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const canWrite = user && CAN_WRITE_CLINICAL.includes(user.role);
  const canRecordLab = user && CAN_RECORD_LAB_RESULTS.includes(user.role);
  const isPatient = user && user.role === 'PATIENT';
  const isDoctor = user && user.role === 'DOCTOR';

  const [patient, setPatient] = useState(null);
  const [visits, setVisits] = useState([]);
  const [history, setHistory] = useState([]);
  const [attachments, setAttachments] = useState([]);
  const [labRequests, setLabRequests] = useState([]);
  const [prescriptions, setPrescriptions] = useState([]);
  const [error, setError] = useState('');

  function loadAll() {
    getPatient(id).then(setPatient).catch(() => setError('Failed to load patient'));
    listVisits(id).then(setVisits).catch(() => {});
    listHistory(id).then(setHistory).catch(() => {});
    listAttachments(id).then(setAttachments).catch(() => {});
    listLabRequests(id).then(setLabRequests).catch(() => {});
    listPrescriptions(id).then(setPrescriptions).catch(() => {});
  }

  useEffect(loadAll, [id]);

  return (
    <AppShell>
      <div className={styles.content}>
        {!isPatient && <Link to="/dashboard#patients-list" className={styles.backLink}>← Back to patients</Link>}
        {error && <p className="error">{error}</p>}

        {patient && (
          <div className={styles.header}>
            <div>
              <h1>{patient.firstName} {patient.lastName}</h1>
              <div className={styles.headerMeta}>
                <span>DOB: {patient.dateOfBirth}</span>
                <span>Sex: {patient.sex}</span>
                {patient.contactPhone && <span>Phone: {patient.contactPhone}</span>}
                {patient.contactEmail && <span>Email: {patient.contactEmail}</span>}
              </div>
            </div>
          </div>
        )}

        <VisitsSection patientId={id} visits={visits} canWrite={canWrite} isDoctor={isDoctor} onChanged={loadAll} />
        <PrescriptionsSection visits={visits} prescriptions={prescriptions} />
        <LabRequestsSection patientId={id} visits={visits} labRequests={labRequests} canRequest={canWrite} canRecord={canRecordLab} onChanged={loadAll} />
        <MedicalHistorySection patientId={id} history={history} canWrite={canWrite} onChanged={loadAll} />
        <AttachmentsSection patientId={id} attachments={attachments} canWrite={canWrite} onChanged={loadAll} />
      </div>
    </AppShell>
  );
}

function VisitsSection({ patientId, visits, canWrite, isDoctor, onChanged }) {
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ visitDate: '', visitType: 'OUTPATIENT', reason: '', notes: '' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await createVisit(patientId, {
        visitDate: new Date(form.visitDate).toISOString(),
        visitType: form.visitType,
        reason: form.reason,
        notes: form.notes || null,
      });
      setShowForm(false);
      setForm({ visitDate: '', visitType: 'OUTPATIENT', reason: '', notes: '' });
      onChanged();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to add visit');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className={styles.section}>
      <div className={styles.sectionHead}>
        <h2>Visit history</h2>
        {canWrite && !showForm && <button onClick={() => setShowForm(true)}>+ Add visit</button>}
      </div>

      <ul className={styles.list}>
        {visits.map((v) => (
          <VisitRow key={v.id} patientId={patientId} visit={v} isDoctor={isDoctor} onChanged={onChanged} />
        ))}
        {visits.length === 0 && <li className={styles.empty}>No visits recorded yet.</li>}
      </ul>

      {showForm && (
        <form className={styles.inlineForm} onSubmit={handleSubmit}>
          <div className={styles.formGrid}>
            <div className="field">
              <label htmlFor="visitDate">Date &amp; time</label>
              <input id="visitDate" type="datetime-local" required
                value={form.visitDate} onChange={(e) => setForm({ ...form, visitDate: e.target.value })} />
            </div>
            <div className="field">
              <label htmlFor="visitType">Type</label>
              <select id="visitType" value={form.visitType} onChange={(e) => setForm({ ...form, visitType: e.target.value })}>
                {VISIT_TYPES.map((t) => <option key={t} value={t}>{t.replace('_', ' ')}</option>)}
              </select>
            </div>
          </div>
          <div className="field">
            <label htmlFor="reason">Reason</label>
            <input id="reason" required value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} />
          </div>
          <div className="field">
            <label htmlFor="notes">Notes</label>
            <input id="notes" value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
          </div>
          {error && <p className="error">{error}</p>}
          <div className={styles.formActions}>
            <button type="button" className={styles.btnSecondary} onClick={() => setShowForm(false)}>Cancel</button>
            <button type="submit" disabled={submitting}>{submitting ? 'Saving…' : 'Save visit'}</button>
          </div>
        </form>
      )}
    </section>
  );
}

const EMPTY_RX = { medicationName: '', dosage: '', frequency: '', durationDays: '', instructions: '' };

function VisitRow({ patientId, visit, isDoctor, onChanged }) {
  const [mode, setMode] = useState(null); // null | 'edit' | 'prescribe'
  const [edit, setEdit] = useState({ status: visit.status, diagnosisCode: '', notes: '' });
  const [rx, setRx] = useState(EMPTY_RX);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  function open(next) {
    setError('');
    if (next === 'edit') {
      setEdit({ status: visit.status, diagnosisCode: visit.diagnosisCode ?? '', notes: visit.notes ?? '' });
    }
    setMode(next);
  }

  async function submit(e, action, failMessage) {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await action();
      setMode(null);
      setRx(EMPTY_RX);
      onChanged();
    } catch (err) {
      setError(err.response?.data?.message || failMessage);
    } finally {
      setSubmitting(false);
    }
  }

  // The API treats null fields as "leave unchanged", so only send what the doctor edited.
  const handleEdit = (e) => submit(e, () => {
    const payload = {};
    if (edit.status !== visit.status) payload.status = edit.status;
    if (edit.diagnosisCode.trim() !== (visit.diagnosisCode ?? '')) payload.diagnosisCode = edit.diagnosisCode.trim();
    if (edit.notes !== (visit.notes ?? '')) payload.notes = edit.notes;
    return updateVisit(patientId, visit.id, payload);
  }, 'Failed to update visit');

  const handlePrescribe = (e) => submit(e, () => createPrescription(patientId, visit.id, {
    medicationName: rx.medicationName,
    dosage: rx.dosage,
    frequency: rx.frequency,
    durationDays: rx.durationDays ? Number(rx.durationDays) : null,
    instructions: rx.instructions || null,
  }), 'Failed to issue prescription');

  return (
    <li className={styles.item}>
      <div className={styles.itemTop}>
        <span>{visit.reason}</span>
        <span className={styles.tagGroup}>
          {visit.status && <span className={styles.itemTagMuted}>{STATUS_LABELS[visit.status] ?? visit.status}</span>}
          <span className={styles.itemTag}>{visit.visitType.replace('_', ' ')}</span>
        </span>
      </div>
      <div className={styles.itemMeta}>
        {new Date(visit.visitDate).toLocaleString()}
        {visit.diagnosisCode && <> · Diagnosis <code>{visit.diagnosisCode}</code></>}
      </div>
      {visit.notes && <div className={styles.itemBody}>{visit.notes}</div>}

      {isDoctor && mode === null && (
        <div className={styles.rowActions}>
          <button type="button" className={styles.btnSecondary} onClick={() => open('edit')}>Update visit</button>
          {visit.status !== 'CANCELLED' && (
            <button type="button" className={styles.btnSecondary} onClick={() => open('prescribe')}>+ Prescribe</button>
          )}
        </div>
      )}

      {mode === 'edit' && (
        <form className={styles.inlineForm} onSubmit={handleEdit}>
          <div className={styles.formGrid}>
            <div className="field">
              <label htmlFor={`status-${visit.id}`}>Status</label>
              <select id={`status-${visit.id}`} value={edit.status} onChange={(e) => setEdit({ ...edit, status: e.target.value })}>
                {VISIT_STATUSES.map((s) => <option key={s} value={s}>{STATUS_LABELS[s]}</option>)}
              </select>
            </div>
            <div className="field">
              <label htmlFor={`dx-${visit.id}`}>Diagnosis code</label>
              <input id={`dx-${visit.id}`} placeholder="e.g. J06.9" value={edit.diagnosisCode}
                onChange={(e) => setEdit({ ...edit, diagnosisCode: e.target.value })} />
            </div>
          </div>
          <div className="field">
            <label htmlFor={`notes-${visit.id}`}>Clinical notes</label>
            <textarea id={`notes-${visit.id}`} rows={3} value={edit.notes}
              onChange={(e) => setEdit({ ...edit, notes: e.target.value })} />
          </div>
          {error && <p className="error">{error}</p>}
          <div className={styles.formActions}>
            <button type="button" className={styles.btnSecondary} onClick={() => setMode(null)}>Cancel</button>
            <button type="submit" disabled={submitting}>{submitting ? 'Saving…' : 'Save changes'}</button>
          </div>
        </form>
      )}

      {mode === 'prescribe' && (
        <form className={styles.inlineForm} onSubmit={handlePrescribe}>
          <div className={styles.formGrid}>
            <div className="field">
              <label htmlFor={`med-${visit.id}`}>Medication</label>
              <input id={`med-${visit.id}`} required value={rx.medicationName}
                onChange={(e) => setRx({ ...rx, medicationName: e.target.value })} />
            </div>
            <div className="field">
              <label htmlFor={`dose-${visit.id}`}>Dosage</label>
              <input id={`dose-${visit.id}`} required placeholder="500 mg" value={rx.dosage}
                onChange={(e) => setRx({ ...rx, dosage: e.target.value })} />
            </div>
            <div className="field">
              <label htmlFor={`freq-${visit.id}`}>Frequency</label>
              <input id={`freq-${visit.id}`} required placeholder="Twice daily" value={rx.frequency}
                onChange={(e) => setRx({ ...rx, frequency: e.target.value })} />
            </div>
            <div className="field">
              <label htmlFor={`days-${visit.id}`}>Duration (days)</label>
              <input id={`days-${visit.id}`} type="number" min="1" value={rx.durationDays}
                onChange={(e) => setRx({ ...rx, durationDays: e.target.value })} />
            </div>
          </div>
          <div className="field">
            <label htmlFor={`instr-${visit.id}`}>Instructions</label>
            <input id={`instr-${visit.id}`} placeholder="Take with food" value={rx.instructions}
              onChange={(e) => setRx({ ...rx, instructions: e.target.value })} />
          </div>
          {error && <p className="error">{error}</p>}
          <div className={styles.formActions}>
            <button type="button" className={styles.btnSecondary} onClick={() => setMode(null)}>Cancel</button>
            <button type="submit" disabled={submitting}>{submitting ? 'Saving…' : 'Issue prescription'}</button>
          </div>
        </form>
      )}
    </li>
  );
}

const RX_STATUS_LABELS = { ACTIVE: 'Active', COMPLETED: 'Completed', CANCELLED: 'Cancelled' };

function PrescriptionsSection({ visits, prescriptions }) {
  const visitById = Object.fromEntries(visits.map((v) => [v.id, v]));

  return (
    <section className={styles.section}>
      <div className={styles.sectionHead}>
        <h2>Prescriptions</h2>
      </div>
      <ul className={styles.list}>
        {prescriptions.map((p) => {
          const visit = visitById[p.visitId];
          return (
            <li key={p.id} className={styles.item}>
              <div className={styles.itemTop}>
                <span>{p.medicationName} · {p.dosage}</span>
                <span className={styles.itemTag}>{RX_STATUS_LABELS[p.status] ?? p.status}</span>
              </div>
              <div className={styles.itemMeta}>
                {p.frequency}
                {p.durationDays ? ` for ${p.durationDays} days` : ''}
                {' · '}issued {new Date(p.createdAt).toLocaleDateString()}
                {visit && ` · visit: ${visit.reason}`}
              </div>
              {p.instructions && <div className={styles.itemBody}>{p.instructions}</div>}
            </li>
          );
        })}
        {prescriptions.length === 0 && <li className={styles.empty}>No prescriptions issued yet.</li>}
      </ul>
    </section>
  );
}

function LabRequestsSection({ patientId, visits, labRequests, canRequest, canRecord, onChanged }) {
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ visitId: '', testType: '', notes: '' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    if (!form.visitId) {
      setError('Select a visit first');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      await createLabRequest(patientId, form.visitId, { testType: form.testType, notes: form.notes || null });
      setShowForm(false);
      setForm({ visitId: '', testType: '', notes: '' });
      onChanged();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to request lab test');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className={styles.section}>
      <div className={styles.sectionHead}>
        <h2>Lab requests</h2>
        {canRequest && !showForm && <button onClick={() => setShowForm(true)}>+ Request lab test</button>}
      </div>

      <ul className={styles.list}>
        {labRequests.map((r) => (
          <LabRequestRow key={r.id} patientId={patientId} request={r} canRecord={canRecord} onChanged={onChanged} />
        ))}
        {labRequests.length === 0 && <li className={styles.empty}>No lab requests yet.</li>}
      </ul>

      {showForm && (
        <form className={styles.inlineForm} onSubmit={handleSubmit}>
          <div className={styles.formGrid}>
            <div className="field">
              <label htmlFor="labVisit">Visit</label>
              <select id="labVisit" required value={form.visitId}
                onChange={(e) => setForm({ ...form, visitId: e.target.value })}>
                <option value="">Select a visit…</option>
                {visits.map((v) => (
                  <option key={v.id} value={v.id}>
                    {new Date(v.visitDate).toLocaleDateString()} — {v.reason}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="testType">Test type</label>
              <input id="testType" required value={form.testType}
                onChange={(e) => setForm({ ...form, testType: e.target.value })} />
            </div>
          </div>
          <div className="field">
            <label htmlFor="labNotes">Notes</label>
            <input id="labNotes" value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
          </div>
          {error && <p className="error">{error}</p>}
          <div className={styles.formActions}>
            <button type="button" className={styles.btnSecondary} onClick={() => setShowForm(false)}>Cancel</button>
            <button type="submit" disabled={submitting}>{submitting ? 'Saving…' : 'Save request'}</button>
          </div>
        </form>
      )}
    </section>
  );
}

function LabRequestRow({ patientId, request, canRecord, onChanged }) {
  const [expanded, setExpanded] = useState(false);
  const [result, setResult] = useState(null);
  const [resultError, setResultError] = useState('');
  const [showResultForm, setShowResultForm] = useState(false);
  const [resultFields, setResultFields] = useState([{ key: '', value: '' }]);
  const [submitting, setSubmitting] = useState(false);

  function toggleExpanded() {
    const next = !expanded;
    setExpanded(next);
    if (next && request.status === 'COMPLETED' && !result) {
      getLabResult(patientId, request.id).then(setResult).catch(() => setResultError('Failed to load result'));
    }
  }

  async function handleCancel() {
    try {
      await updateLabRequestStatus(patientId, request.id, 'CANCELLED');
      onChanged();
    } catch (err) {
      setResultError(err.response?.data?.message || 'Failed to cancel request');
    }
  }

  async function handleRecordResult(e) {
    e.preventDefault();
    setSubmitting(true);
    setResultError('');
    try {
      const resultData = Object.fromEntries(
        resultFields.filter((f) => f.key.trim() !== '').map((f) => [f.key, f.value])
      );
      const saved = await recordLabResult(patientId, request.id, resultData);
      setResult(saved);
      setShowResultForm(false);
      onChanged();
    } catch (err) {
      setResultError(err.response?.data?.message || 'Failed to record result');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <li className={styles.item}>
      <div className={styles.itemTop} onClick={toggleExpanded} style={{ cursor: 'pointer' }}>
        <span>{request.testType}</span>
        <span className={styles.itemTag}>{LAB_STATUS_LABELS[request.status] ?? request.status}</span>
      </div>
      <div className={styles.itemMeta}>{new Date(request.createdAt).toLocaleString()}</div>
      {request.notes && <div className={styles.itemBody}>{request.notes}</div>}

      {expanded && (
        <div className={styles.itemBody}>
          {resultError && <p className="error">{resultError}</p>}

          {request.status === 'COMPLETED' && result && (
            <>
              <table className={styles.table}>
                <tbody>
                  {Object.entries(result.resultData).map(([k, v]) => (
                    <tr key={k}><td>{k}</td><td>{String(v)}</td></tr>
                  ))}
                </tbody>
              </table>
              <button type="button" className={styles.btnSecondary} onClick={() => window.print()}>Print report</button>
            </>
          )}

          {canRecord && request.status !== 'COMPLETED' && request.status !== 'CANCELLED' && !showResultForm && (
            <div className={styles.formActions}>
              <button type="button" onClick={() => setShowResultForm(true)}>Record result</button>
              <button type="button" className={styles.btnSecondary} onClick={handleCancel}>Cancel request</button>
            </div>
          )}

          {showResultForm && (
            <form className={styles.inlineForm} onSubmit={handleRecordResult}>
              {resultFields.map((f, i) => (
                <div className={styles.formGrid} key={i}>
                  <div className="field">
                    <label>Field</label>
                    <input value={f.key} onChange={(e) => {
                      const next = [...resultFields];
                      next[i] = { ...next[i], key: e.target.value };
                      setResultFields(next);
                    }} />
                  </div>
                  <div className="field">
                    <label>Value</label>
                    <input value={f.value} onChange={(e) => {
                      const next = [...resultFields];
                      next[i] = { ...next[i], value: e.target.value };
                      setResultFields(next);
                    }} />
                  </div>
                </div>
              ))}
              <button type="button" className={styles.btnSecondary} onClick={() => setResultFields([...resultFields, { key: '', value: '' }])}>
                + Add field
              </button>
              <div className={styles.formActions}>
                <button type="button" className={styles.btnSecondary} onClick={() => setShowResultForm(false)}>Cancel</button>
                <button type="submit" disabled={submitting}>{submitting ? 'Saving…' : 'Save result'}</button>
              </div>
            </form>
          )}
        </div>
      )}
    </li>
  );
}

function MedicalHistorySection({ patientId, history, canWrite, onChanged }) {
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ category: 'CONDITION', description: '', recordedDate: '' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await createHistoryEntry(patientId, form);
      setShowForm(false);
      setForm({ category: 'CONDITION', description: '', recordedDate: '' });
      onChanged();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to add entry');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className={styles.section}>
      <div className={styles.sectionHead}>
        <h2>Medical history</h2>
        {canWrite && !showForm && <button onClick={() => setShowForm(true)}>+ Add entry</button>}
      </div>

      <ul className={styles.list}>
        {history.map((h) => (
          <li key={h.id} className={styles.item}>
            <div className={styles.itemTop}>
              <span>{h.description}</span>
              <span className={styles.itemTag}>{h.category.replace('_', ' ')}</span>
            </div>
            <div className={styles.itemMeta}>Recorded {h.recordedDate}</div>
          </li>
        ))}
        {history.length === 0 && <li className={styles.empty}>No medical history recorded yet.</li>}
      </ul>

      {showForm && (
        <form className={styles.inlineForm} onSubmit={handleSubmit}>
          <div className={styles.formGrid}>
            <div className="field">
              <label htmlFor="category">Category</label>
              <select id="category" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
                {HISTORY_CATEGORIES.map((c) => <option key={c} value={c}>{c.replace('_', ' ')}</option>)}
              </select>
            </div>
            <div className="field">
              <label htmlFor="recordedDate">Date</label>
              <input id="recordedDate" type="date" required
                value={form.recordedDate} onChange={(e) => setForm({ ...form, recordedDate: e.target.value })} />
            </div>
          </div>
          <div className="field">
            <label htmlFor="description">Description</label>
            <input id="description" required value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </div>
          {error && <p className="error">{error}</p>}
          <div className={styles.formActions}>
            <button type="button" className={styles.btnSecondary} onClick={() => setShowForm(false)}>Cancel</button>
            <button type="submit" disabled={submitting}>{submitting ? 'Saving…' : 'Save entry'}</button>
          </div>
        </form>
      )}
    </section>
  );
}

function AttachmentsSection({ patientId, attachments, canWrite, onChanged }) {
  const [showForm, setShowForm] = useState(false);
  const [file, setFile] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  async function handleUpload(e) {
    e.preventDefault();
    if (!file) return;
    setSubmitting(true);
    setError('');
    try {
      await uploadAttachment(patientId, file);
      setShowForm(false);
      setFile(null);
      onChanged();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to upload file');
    } finally {
      setSubmitting(false);
    }
  }

  function formatSize(bytes) {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  return (
    <section className={styles.section}>
      <div className={styles.sectionHead}>
        <h2>Attachments</h2>
        {canWrite && !showForm && <button onClick={() => setShowForm(true)}>+ Upload file</button>}
      </div>

      <ul className={styles.list}>
        {attachments.map((a) => (
          <li key={a.id} className={styles.item}>
            <div className={styles.itemTop}>
              <span>{a.fileName}</span>
              <button className={styles.downloadLink} onClick={() => downloadAttachment(patientId, a.id, a.fileName)}>
                Download
              </button>
            </div>
            <div className={styles.itemMeta}>{formatSize(a.fileSizeBytes)} · {new Date(a.createdAt).toLocaleString()}</div>
          </li>
        ))}
        {attachments.length === 0 && <li className={styles.empty}>No files attached yet.</li>}
      </ul>

      {showForm && (
        <form className={styles.inlineForm} onSubmit={handleUpload}>
          <div className="field">
            <label htmlFor="file">Choose a file</label>
            <input id="file" type="file" required onChange={(e) => setFile(e.target.files[0])} />
          </div>
          {error && <p className="error">{error}</p>}
          <div className={styles.formActions}>
            <button type="button" className={styles.btnSecondary} onClick={() => setShowForm(false)}>Cancel</button>
            <button type="submit" disabled={submitting || !file}>{submitting ? 'Uploading…' : 'Upload'}</button>
          </div>
        </form>
      )}
    </section>
  );
}
