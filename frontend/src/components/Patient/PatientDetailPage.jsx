import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getPatient } from '../../services/patientService.js';
import { listVisits, createVisit } from '../../services/visitService.js';
import { listHistory, createHistoryEntry } from '../../services/medicalHistoryService.js';
import { listAttachments, uploadAttachment, downloadAttachment } from '../../services/attachmentService.js';
import { useAuth } from '../../context/AuthContext.jsx';
import styles from './PatientDetailPage.module.css';

const VISIT_TYPES = ['OUTPATIENT', 'INPATIENT', 'EMERGENCY', 'FOLLOW_UP'];
const HISTORY_CATEGORIES = ['CONDITION', 'ALLERGY', 'MEDICATION', 'SURGERY', 'IMMUNIZATION', 'FAMILY_HISTORY'];
const CAN_WRITE_CLINICAL = ['ADMIN', 'NURSE', 'DOCTOR'];

export default function PatientDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const canWrite = user && CAN_WRITE_CLINICAL.includes(user.role);
  const isPatient = user && user.role === 'PATIENT';

  const [patient, setPatient] = useState(null);
  const [visits, setVisits] = useState([]);
  const [history, setHistory] = useState([]);
  const [attachments, setAttachments] = useState([]);
  const [error, setError] = useState('');

  function loadAll() {
    getPatient(id).then(setPatient).catch(() => setError('Failed to load patient'));
    listVisits(id).then(setVisits).catch(() => {});
    listHistory(id).then(setHistory).catch(() => {});
    listAttachments(id).then(setAttachments).catch(() => {});
  }

  useEffect(loadAll, [id]);

  return (
    <div className={styles.page}>
      <header className={styles.topbar}>
        {!isPatient && <Link to="/dashboard" className={styles.backLink}>← Back to patients</Link>}
      </header>

      <div className={styles.content}>
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

        <VisitsSection patientId={id} visits={visits} canWrite={canWrite} onChanged={loadAll} />
        <MedicalHistorySection patientId={id} history={history} canWrite={canWrite} onChanged={loadAll} />
        <AttachmentsSection patientId={id} attachments={attachments} canWrite={canWrite} onChanged={loadAll} />
      </div>
    </div>
  );
}

function VisitsSection({ patientId, visits, canWrite, onChanged }) {
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
          <li key={v.id} className={styles.item}>
            <div className={styles.itemTop}>
              <span>{v.reason}</span>
              <span className={styles.itemTag}>{v.visitType.replace('_', ' ')}</span>
            </div>
            <div className={styles.itemMeta}>{new Date(v.visitDate).toLocaleString()}</div>
            {v.notes && <div className={styles.itemBody}>{v.notes}</div>}
          </li>
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
