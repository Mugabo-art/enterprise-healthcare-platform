import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getSchedule } from '../../services/doctorService.js';
import { getPatient } from '../../services/patientService.js';
import { updateVisit } from '../../services/visitService.js';
import { useAuth } from '../../context/AuthContext.jsx';
import AppShell from '../Layout/AppShell.jsx';
import { STATUS_LABELS, NEXT_STATUS_ACTIONS } from './visitStatus.js';
import styles from './SchedulePage.module.css';

const VIEWS = [
  { key: 'today', label: 'Today' },
  { key: 'upcoming', label: 'Upcoming' },
  { key: 'past', label: 'Past' },
];

function startOfDay(date) {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  return d;
}

// Splits the schedule relative to the viewer's local day. Upcoming reads soonest-first;
// past keeps the API's newest-first order.
// eslint-disable-next-line react-refresh/only-export-components -- exported for unit tests
export function filterByView(visits, view, now = new Date()) {
  const today = startOfDay(now).getTime();
  const tomorrow = today + 24 * 60 * 60 * 1000;
  const at = (v) => new Date(v.visitDate).getTime();
  if (view === 'today') return visits.filter((v) => at(v) >= today && at(v) < tomorrow).sort((a, b) => at(a) - at(b));
  if (view === 'upcoming') return visits.filter((v) => at(v) >= tomorrow).sort((a, b) => at(a) - at(b));
  return visits.filter((v) => at(v) < today);
}

export default function SchedulePage() {
  const { user } = useAuth();
  const [visits, setVisits] = useState([]);
  const [patients, setPatients] = useState({});
  const [view, setView] = useState('today');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  function load() {
    if (!user) return;
    getSchedule(user.id)
      .then((data) => {
        setVisits(data);
        // The schedule only carries patient ids; resolve each distinct one once for display.
        const ids = [...new Set(data.map((v) => v.patientId))];
        ids.forEach((pid) => {
          getPatient(pid)
            .then((p) => setPatients((prev) => ({ ...prev, [pid]: p })))
            .catch(() => {});
        });
      })
      .catch(() => setError('Failed to load schedule'))
      .finally(() => setLoading(false));
  }

  useEffect(load, [user?.id]); // eslint-disable-line react-hooks/exhaustive-deps -- reload only when the signed-in doctor changes

  async function handleStatus(visit, status) {
    setError('');
    try {
      const updated = await updateVisit(visit.patientId, visit.id, { status });
      setVisits((prev) => prev.map((v) => (v.id === updated.id ? updated : v)));
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update visit');
    }
  }

  const shown = filterByView(visits, view);
  const todayCount = filterByView(visits, 'today').length;
  const openToday = filterByView(visits, 'today').filter((v) => v.status === 'SCHEDULED' || v.status === 'IN_PROGRESS').length;

  return (
    <AppShell>
      <div className={styles.content}>
        <div className={styles.header}>
          <div>
            <h1>My schedule</h1>
            <p className={styles.subtitle}>
              {todayCount === 0
                ? 'No visits today.'
                : `${todayCount} visit${todayCount > 1 ? 's' : ''} today · ${openToday} still open`}
            </p>
          </div>
          <div className={styles.tabs} role="tablist">
            {VIEWS.map((v) => (
              <button
                key={v.key}
                type="button"
                role="tab"
                aria-selected={view === v.key}
                className={`${styles.tab} ${view === v.key ? styles.tabActive : ''}`}
                onClick={() => setView(v.key)}
              >
                {v.label}
              </button>
            ))}
          </div>
        </div>

        {error && <p className="error">{error}</p>}

        <section className={styles.section}>
          <ul className={styles.list}>
            {shown.map((v) => (
              <ScheduleRow key={v.id} visit={v} patient={patients[v.patientId]} showDate={view !== 'today'} onStatus={handleStatus} />
            ))}
            {!loading && shown.length === 0 && <li className={styles.empty}>Nothing scheduled here.</li>}
          </ul>
        </section>
      </div>
    </AppShell>
  );
}

function ScheduleRow({ visit, patient, showDate, onStatus }) {
  const when = new Date(visit.visitDate);
  const actions = NEXT_STATUS_ACTIONS[visit.status] ?? [];
  const patientName = patient ? `${patient.firstName} ${patient.lastName}` : 'Loading patient…';

  return (
    <li className={styles.item}>
      <div className={styles.time}>
        {showDate && <span className={styles.date}>{when.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}</span>}
        <span>{when.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}</span>
      </div>
      <div className={styles.body}>
        <div className={styles.itemTop}>
          <Link to={`/dashboard/patients/${visit.patientId}`} className={styles.patientLink}>{patientName}</Link>
          <span className={`${styles.status} ${styles[`status_${visit.status}`] ?? ''}`}>{STATUS_LABELS[visit.status] ?? visit.status}</span>
        </div>
        <div className={styles.itemMeta}>
          {visit.visitType.replace('_', ' ')} · {visit.reason}
          {visit.diagnosisCode && <> · Dx <code>{visit.diagnosisCode}</code></>}
        </div>
      </div>
      {actions.length > 0 && (
        <div className={styles.actions}>
          {actions.map((a) => (
            <button
              key={a.status}
              type="button"
              className={a.secondary ? styles.btnSecondary : undefined}
              onClick={() => onStatus(visit, a.status)}
            >
              {a.label}
            </button>
          ))}
        </div>
      )}
    </li>
  );
}
