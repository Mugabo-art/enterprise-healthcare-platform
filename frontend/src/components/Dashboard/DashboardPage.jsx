import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listPatients, createPatient } from '../../services/patientService.js';
import { getDashboardAnalytics } from '../../services/analyticsService.js';
import { useAuth } from '../../context/AuthContext.jsx';
import StatTile from './charts/StatTile.jsx';
import TrendChart from './charts/TrendChart.jsx';
import BreakdownBarChart from './charts/BreakdownBarChart.jsx';
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

const emptyForm = { firstName: '', lastName: '', dateOfBirth: '', sex: 'MALE', contactPhone: '', contactEmail: '', address: '' };

export default function DashboardPage() {
  const [patients, setPatients] = useState([]);
  const [search, setSearch] = useState('');
  const [error, setError] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);
  const [analytics, setAnalytics] = useState(null);
  const [analyticsError, setAnalyticsError] = useState('');
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const canManagePatients = user && CAN_MANAGE_PATIENTS.includes(user.role);
  const isPatient = user && user.role === 'PATIENT';

  function loadPatients() {
    if (isPatient) return;
    listPatients(search)
      .then((page) => setPatients(page.content ?? []))
      .catch((err) => setError(err.response?.data?.message || 'Failed to load patients'));
  }

  useEffect(loadPatients, [search, isPatient]);

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

  async function handleLogout() {
    await logout();
    navigate('/');
  }

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

  return (
    <div className={styles.page}>
      <header className={styles.topbar}>
        <span className={styles.brand}>
          <span className={styles.mark}>
            <svg viewBox="0 0 24 24"><path d="M12 2v20M2 12h20" /></svg>
          </span>
          MediCore
        </span>
        {user && (
          <div className={styles.userInfo}>
            <div className={styles.userBadge}>
              <span className={styles.userEmail}>{user.email}</span>
              <span className={styles.userRole}>{user.role}</span>
            </div>
            <button className={styles.logoutBtn} onClick={handleLogout}>Log out</button>
          </div>
        )}
      </header>

      <div className={styles.content}>
        {isPatient ? (
          <div className={styles.card}>
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
        ) : (
        <>
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
                label={analytics.selfScoped ? 'My patients' : 'Total patients'}
                value={analytics.totalPatients}
              />
              <StatTile
                label="New in last 30 days"
                value={analytics.newPatientsLast30Days}
                hint={analytics.selfScoped ? 'Patients you’ve newly seen' : 'New registrations'}
              />
              <StatTile label="Total visits" value={analytics.totalVisits} />
              <StatTile label="Total prescriptions" value={analytics.totalPrescriptions} />
            </div>

            <div className={styles.chartCard}>
              <h2 className={styles.chartTitle}>Visits, last 14 days</h2>
              <TrendChart data={analytics.visitsTrend} title="Visits over the last 14 days" />
            </div>

            <div className={styles.chartsGrid}>
              <div className={styles.chartCard}>
                <h2 className={styles.chartTitle}>Visits by status</h2>
                <BreakdownBarChart
                  title="Visits by status"
                  categories={toChartCategories(analytics.visitsByStatus, VISIT_STATUS_LABELS)}
                />
              </div>
              <div className={styles.chartCard}>
                <h2 className={styles.chartTitle}>Visits by type</h2>
                <BreakdownBarChart
                  title="Visits by type"
                  categories={toChartCategories(analytics.visitsByType, VISIT_TYPE_LABELS)}
                />
              </div>
              <div className={styles.chartCard}>
                <h2 className={styles.chartTitle}>Prescriptions by status</h2>
                <BreakdownBarChart
                  title="Prescriptions by status"
                  categories={toChartCategories(analytics.prescriptionsByStatus, PRESCRIPTION_STATUS_LABELS)}
                />
              </div>
            </div>
          </>
        )}

        <div className={styles.headRow}>
          <h1>Patients</h1>
          {canManagePatients && (
            <button onClick={() => setShowForm(true)}>+ Add patient</button>
          )}
        </div>

        <div className={styles.searchRow}>
          <input
            placeholder="Search by last name"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        {error && <p className="error">{error}</p>}

        <div className={styles.card}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Name</th>
                <th>Date of birth</th>
                <th>Sex</th>
                <th>Contact</th>
              </tr>
            </thead>
            <tbody>
              {patients.map((p) => (
                <tr key={p.id} onClick={() => navigate(`/dashboard/patients/${p.id}`)}>
                  <td>{p.firstName} {p.lastName}</td>
                  <td>{p.dateOfBirth}</td>
                  <td>{p.sex}</td>
                  <td>{p.contactPhone || p.contactEmail || '—'}</td>
                </tr>
              ))}
              {patients.length === 0 && !error && (
                <tr><td colSpan={4} className={styles.emptyRow}>No patients found.</td></tr>
              )}
            </tbody>
          </table>
        </div>
        </>
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
    </div>
  );
}
