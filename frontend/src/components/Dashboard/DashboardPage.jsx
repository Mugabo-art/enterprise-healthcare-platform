import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { listPatients, createPatient } from '../../services/patientService.js';
import { getDashboardAnalytics } from '../../services/analyticsService.js';
import { useAuth } from '../../context/AuthContext.jsx';
import AppShell from '../Layout/AppShell.jsx';
import { Icon, icons } from '../Layout/icons.jsx';
import StatTile from './charts/StatTile.jsx';
import TrendChart from './charts/TrendChart.jsx';
import { categoricalColor } from './charts/palette.js';
import styles from './DashboardPage.module.css';

const VISIT_STATUS_LABELS = { SCHEDULED: 'Scheduled', IN_PROGRESS: 'In progress', COMPLETED: 'Completed', CANCELLED: 'Cancelled' };
const VISIT_TYPE_LABELS = { OUTPATIENT: 'Outpatient', INPATIENT: 'Inpatient', EMERGENCY: 'Emergency', FOLLOW_UP: 'Follow-up' };
const PRESCRIPTION_STATUS_LABELS = { ACTIVE: 'Active', COMPLETED: 'Completed', CANCELLED: 'Cancelled' };

function toChartCategories(labelCounts, labelMap) {
  return labelCounts.map((lc, i) => ({
    label: labelMap[lc.label] ?? lc.label,
    value: lc.count,
    color: categoricalColor(i),
  }));
}

const SEX_OPTIONS = ['MALE', 'FEMALE', 'OTHER'];
const CAN_MANAGE_PATIENTS = ['ADMIN', 'NURSE'];
const PAGE_SIZE = 8;
const SORT_COLUMNS = [
  { key: 'lastName', label: 'Name' },
  { key: 'dateOfBirth', label: 'Date of birth' },
];

const emptyForm = { firstName: '', lastName: '', dateOfBirth: '', sex: 'MALE', contactPhone: '', contactEmail: '', address: '' };

function initials(firstName, lastName) {
  return `${(firstName?.[0] ?? '').toUpperCase()}${(lastName?.[0] ?? '').toUpperCase()}` || '?';
}

function shortId(id) {
  return `#${id.slice(0, 6).toUpperCase()}`;
}

function toCsv(rows) {
  const header = ['First name', 'Last name', 'Date of birth', 'Sex', 'Phone', 'Email'];
  const lines = rows.map((p) => [p.firstName, p.lastName, p.dateOfBirth, p.sex, p.contactPhone ?? '', p.contactEmail ?? '']
    .map((v) => `"${String(v).replace(/"/g, '""')}"`).join(','));
  return [header.join(','), ...lines].join('\n');
}

export default function DashboardPage() {
  const [searchParams] = useSearchParams();
  const [patientsPage, setPatientsPage] = useState(null);
  const [search, setSearch] = useState(searchParams.get('q') || '');
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState({ key: 'lastName', dir: 'asc' });
  const [error, setError] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);
  const [analytics, setAnalytics] = useState(null);
  const [analyticsError, setAnalyticsError] = useState('');
  const { user } = useAuth();
  const navigate = useNavigate();

  const canManagePatients = user && CAN_MANAGE_PATIENTS.includes(user.role);
  const isPatient = user && user.role === 'PATIENT';

  function loadPatients() {
    if (isPatient) return;
    listPatients(search, page, PAGE_SIZE, `${sort.key},${sort.dir}`)
      .then(setPatientsPage)
      .catch((err) => setError(err.response?.data?.message || 'Failed to load patients'));
  }

  useEffect(loadPatients, [search, page, sort, isPatient]);
  useEffect(() => setPage(0), [search, sort]);

  // Keeps the topbar's global search in sync even when it's used while
  // already on /dashboard (a fresh navigation already picks up ?q= via the
  // useState initializer above, but that doesn't re-fire on a same-page nav).
  useEffect(() => {
    const q = searchParams.get('q');
    if (q !== null) setSearch(q);
  }, [searchParams]);

  useEffect(() => {
    if (isPatient) return;
    getDashboardAnalytics()
      .then(setAnalytics)
      .catch((err) => setAnalyticsError(err.response?.data?.message || 'Failed to load dashboard analytics'));
  }, [isPatient]);

  useEffect(() => {
    if (isPatient && user.patientId) {
      navigate(`/dashboard/patients/${user.patientId}`, { replace: true });
    }
  }, [isPatient, user, navigate]);

  const patients = patientsPage?.content ?? [];

  async function handleCreatePatient(e) {
    e.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await createPatient(form);
      setShowForm(false);
      setForm(emptyForm);
      loadPatients();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create patient');
    } finally {
      setSubmitting(false);
    }
  }

  function toggleSort(key) {
    setSort((s) => (s.key === key ? { key, dir: s.dir === 'asc' ? 'desc' : 'asc' } : { key, dir: 'asc' }));
  }

  function handleExport() {
    const blob = new Blob([toCsv(patients)], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'patients.csv';
    a.click();
    URL.revokeObjectURL(url);
  }

  const rangeLabel = useMemo(() => {
    if (!patientsPage || patientsPage.totalElements === 0) return null;
    const start = patientsPage.number * patientsPage.size + 1;
    const end = Math.min(start + patients.length - 1, patientsPage.totalElements);
    return `Showing ${start}–${end} of ${patientsPage.totalElements}`;
  }, [patientsPage, patients.length]);

  if (isPatient) {
    return (
      <AppShell>
        <div className={styles.card} style={{ padding: 28 }}>
          <h1>Welcome</h1>
          {user.patientId ? (
            <p>Loading your record…</p>
          ) : (
            <p>
              Your account isn't linked to a patient record yet. A staff member needs to
              link it before you can see your visits, history, and prescriptions here.
            </p>
          )}
        </div>
      </AppShell>
    );
  }

  return (
    <AppShell>
      <div className={styles.headRow}>
        <h1>Overview</h1>
        {analytics && (
          <span className={styles.scopeBadge}>
            {analytics.selfScoped ? 'Your patient load' : 'Hospital-wide'}
          </span>
        )}
      </div>

      {analyticsError && <p className="error">{analyticsError}</p>}

      {analytics && (
        <>
          <div className={styles.kpiGrid}>
            <StatTile
              icon={icons.users}
              label={analytics.selfScoped ? 'My patients' : 'Total patients'}
              value={analytics.totalPatients}
              badge={analytics.newPatientsLast30Days > 0 ? (
                <><Icon path={icons.trendingUp} className={styles.badgeIcon} /> +{analytics.newPatientsLast30Days} this month</>
              ) : null}
              hint={analytics.newPatientsLast30Days === 0 ? (analytics.selfScoped ? 'Patients you’ve newly seen' : 'New registrations') : undefined}
            />
            <StatTile
              icon={icons.calendar}
              label="Total visits"
              value={analytics.totalVisits}
              breakdown={toChartCategories(analytics.visitsByStatus, VISIT_STATUS_LABELS)}
            />
            <StatTile
              icon={icons.pill}
              label="Total prescriptions"
              value={analytics.totalPrescriptions}
              breakdown={toChartCategories(analytics.prescriptionsByStatus, PRESCRIPTION_STATUS_LABELS)}
            />
            <StatTile
              icon={icons.activity}
              label="Visits by type"
              value={analytics.totalVisits}
              breakdown={toChartCategories(analytics.visitsByType, VISIT_TYPE_LABELS)}
            />
          </div>

          <div className={styles.chartCard}>
            <h2 className={styles.chartTitle}>Visits, last 14 days</h2>
            <TrendChart data={analytics.visitsTrend} title="Visits over the last 14 days" />
          </div>
        </>
      )}

      <div className={styles.headRow} id="patients-list">
        <h1>Patients</h1>
        {canManagePatients && (
          <button onClick={() => setShowForm(true)}>+ Add patient</button>
        )}
      </div>

      <div className={styles.toolbar}>
        <div className={styles.searchRow}>
          <input
            placeholder="Search by last name"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <button type="button" className={styles.btnSecondary} onClick={handleExport} disabled={patients.length === 0}>
          <Icon path={icons.download} className={styles.toolbarIcon} /> Export CSV
        </button>
      </div>

      {error && <p className="error">{error}</p>}

      <div className={styles.card}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>ID</th>
              {SORT_COLUMNS.map((col) => (
                <th key={col.key} className={styles.sortableHeader} onClick={() => toggleSort(col.key)}>
                  {col.label}
                  {sort.key === col.key && (
                    <Icon path={sort.dir === 'asc' ? icons.chevronUp : icons.chevronDown} className={styles.sortIcon} />
                  )}
                </th>
              ))}
              <th>Sex</th>
              <th>Contact</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {patients.map((p) => (
              <tr key={p.id} onClick={() => navigate(`/dashboard/patients/${p.id}`)}>
                <td className={styles.idCell}>{shortId(p.id)}</td>
                <td>
                  <div className={styles.nameCell}>
                    <span className={styles.rowAvatar}>{initials(p.firstName, p.lastName)}</span>
                    {p.firstName} {p.lastName}
                  </div>
                </td>
                <td>{p.dateOfBirth}</td>
                <td><span className={styles.sexTag}>{p.sex}</span></td>
                <td>{p.contactPhone || p.contactEmail || '—'}</td>
                <td className={styles.rowAction}>
                  <button
                    type="button"
                    className={styles.rowActionBtn}
                    onClick={(e) => { e.stopPropagation(); navigate(`/dashboard/patients/${p.id}`); }}
                    aria-label={`View ${p.firstName} ${p.lastName}`}
                  >
                    <Icon path={icons.edit} className={styles.toolbarIcon} />
                  </button>
                </td>
              </tr>
            ))}
            {patients.length === 0 && !error && (
              <tr><td colSpan={6} className={styles.emptyRow}>No patients found.</td></tr>
            )}
          </tbody>
        </table>

        {patientsPage && patientsPage.totalElements > 0 && (
          <div className={styles.pagination}>
            <span className={styles.rangeLabel}>{rangeLabel}</span>
            <div className={styles.pageControls}>
              <button type="button" className={styles.iconBtnSmall} disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                <Icon path={icons.chevronLeft} className={styles.toolbarIcon} />
              </button>
              <span>Page {patientsPage.number + 1} of {Math.max(patientsPage.totalPages, 1)}</span>
              <button type="button" className={styles.iconBtnSmall} disabled={page + 1 >= patientsPage.totalPages} onClick={() => setPage((p) => p + 1)}>
                <Icon path={icons.chevronRight} className={styles.toolbarIcon} />
              </button>
            </div>
          </div>
        )}
      </div>

      {showForm && (
        <div className={styles.formOverlay} onClick={() => setShowForm(false)}>
          <div className={styles.formCard} onClick={(e) => e.stopPropagation()}>
            <h2>Add patient</h2>
            <form onSubmit={handleCreatePatient}>
              <div className={styles.formGrid}>
                <div className="field">
                  <label htmlFor="firstName">First name</label>
                  <input id="firstName" required value={form.firstName}
                    onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
                </div>
                <div className="field">
                  <label htmlFor="lastName">Last name</label>
                  <input id="lastName" required value={form.lastName}
                    onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
                </div>
              </div>
              <div className={styles.formGrid}>
                <div className="field">
                  <label htmlFor="dateOfBirth">Date of birth</label>
                  <input id="dateOfBirth" type="date" required value={form.dateOfBirth}
                    onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })} />
                </div>
                <div className="field">
                  <label htmlFor="sex">Sex</label>
                  <select id="sex" value={form.sex} onChange={(e) => setForm({ ...form, sex: e.target.value })}>
                    {SEX_OPTIONS.map((s) => <option key={s} value={s}>{s}</option>)}
                  </select>
                </div>
              </div>
              <div className="field">
                <label htmlFor="contactPhone">Contact phone</label>
                <input id="contactPhone" value={form.contactPhone}
                  onChange={(e) => setForm({ ...form, contactPhone: e.target.value })} />
              </div>
              <div className="field">
                <label htmlFor="contactEmail">Contact email</label>
                <input id="contactEmail" type="email" value={form.contactEmail}
                  onChange={(e) => setForm({ ...form, contactEmail: e.target.value })} />
              </div>
              <div className="field">
                <label htmlFor="address">Address</label>
                <input id="address" value={form.address}
                  onChange={(e) => setForm({ ...form, address: e.target.value })} />
              </div>
              {error && <p className="error">{error}</p>}
              <div className={styles.formActions}>
                <button type="button" className={styles.btnSecondary} onClick={() => setShowForm(false)}>Cancel</button>
                <button type="submit" disabled={submitting}>{submitting ? 'Saving…' : 'Save patient'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  );
}
