import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import * as authService from '../../services/authService.js';
import { useAuth } from '../../context/AuthContext.jsx';
import AuthLayout from '../common/AuthLayout.jsx';

const ROLES = [
  { value: 'ADMIN', label: 'Administrator' },
  { value: 'DOCTOR', label: 'Doctor' },
  { value: 'NURSE', label: 'Nurse' },
  { value: 'LAB_TECH', label: 'Lab Technician' },
  { value: 'PHARMACIST', label: 'Pharmacist' },
  { value: 'BILLING_CLERK', label: 'Billing Clerk' },
  { value: 'PATIENT', label: 'Patient' },
];

export default function RegisterPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const { user } = useAuth();
  // Only a signed-in administrator may create staff accounts; everyone else self-registers as a patient.
  const isAdmin = user?.role === 'ADMIN';
  const [role, setRole] = useState('PATIENT');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    if (password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setSubmitting(true);
    try {
      await authService.register(email, password, isAdmin ? role : 'PATIENT');
      // Registration succeeds but does not log the user in (the backend issues
      // no tokens on /auth/register — see docs/API_DOCUMENTATION.md) so we send
      // them to log in with their new credentials.
      navigate(isAdmin ? '/dashboard' : '/login', { state: { justRegistered: true } });
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed. Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout>
      <h2 style={{ marginTop: 0 }}>Create your account</h2>
      <p style={{ color: '#4B5D55', marginTop: -8, fontSize: '0.92rem' }}>
        {isAdmin
          ? 'Create a staff or patient account.'
          : 'Create a patient portal account. Staff accounts are created by a hospital administrator.'}
      </p>
      <form onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="email">Work email</label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoFocus
          />
        </div>
        {isAdmin && (
        <div className="field">
          <label htmlFor="role">Role</label>
          <select id="role" value={role} onChange={(e) => setRole(e.target.value)}>
            {ROLES.map((r) => (
              <option key={r.value} value={r.value}>{r.label}</option>
            ))}
          </select>
          {role === 'PATIENT' && (
            <p style={{ color: '#4B5D55', fontSize: '0.85rem', marginTop: 6, marginBottom: 0 }}>
              A staff member will need to link this account to your patient record before your portal shows any data.
            </p>
          )}
        </div>
        )}
        {!isAdmin && (
          <p style={{ color: '#4B5D55', fontSize: '0.85rem', marginTop: 0 }}>
            A staff member will need to link this account to your patient record before your portal shows any data.
          </p>
        )}
        <div className="field">
          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
          />
        </div>
        <div className="field">
          <label htmlFor="confirmPassword">Confirm password</label>
          <input
            id="confirmPassword"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
            minLength={8}
          />
        </div>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={submitting} style={{ width: '100%' }}>
          {submitting ? 'Creating account…' : 'Create account'}
        </button>
      </form>
      <p style={{ fontSize: '0.9rem', marginTop: 16 }}>
        Already have an account? <Link to="/login">Log in</Link>
      </p>
    </AuthLayout>
  );
}
