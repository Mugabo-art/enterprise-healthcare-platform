import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listPatients, createPatient } from '../../services/patientService.js';
import { useAuth } from '../../context/AuthContext.jsx';
import styles from './DashboardPage.module.css';

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
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const canManagePatients = user && CAN_MANAGE_PATIENTS.includes(user.role);

  function loadPatients() {
    listPatients(search)
      .then((page) => setPatients(page.content ?? []))
      .catch((err) => setError(err.response?.data?.message || 'Failed to load patients'));
  }

  useEffect(loadPatients, [search]);

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
