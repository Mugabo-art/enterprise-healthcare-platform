import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import AuthLayout from '../common/AuthLayout.jsx';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [mfaChallenge, setMfaChallenge] = useState(null);
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { login, completeMfa } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const justRegistered = location.state?.justRegistered;

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const result = await login(email, password);
      if (result.mfaRequired) {
        setMfaChallenge(result.challengeId);
      } else {
        navigate('/dashboard');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleMfaSubmit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await completeMfa(mfaChallenge, code);
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid code');
    } finally {
      setSubmitting(false);
    }
  }

  if (mfaChallenge) {
    return (
      <AuthLayout>
        <h2 style={{ marginTop: 0 }}>Enter your MFA code</h2>
        <p style={{ color: '#4B5D55', marginTop: -8, fontSize: '0.92rem' }}>
          Enter the 6-digit code from your authenticator app.
        </p>
        <form onSubmit={handleMfaSubmit}>
          <div className="field">
            <label htmlFor="code">6-digit code</label>
            <input id="code" value={code} onChange={(e) => setCode(e.target.value)} autoFocus />
          </div>
          {error && <p className="error">{error}</p>}
          <button type="submit" disabled={submitting} style={{ width: '100%' }}>
            {submitting ? 'Verifying…' : 'Verify'}
          </button>
        </form>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout>
      <h2 style={{ marginTop: 0 }}>Welcome back</h2>
      <p style={{ color: '#4B5D55', marginTop: -8, fontSize: '0.92rem' }}>
        Log in to your MediCore workspace.
      </p>
      {justRegistered && (
        <p style={{ color: '#0F7A38', background: '#ECFDF3', padding: '10px 14px', borderRadius: 8, fontSize: '0.9rem' }}>
          Account created — log in with your new credentials.
        </p>
      )}
      <form onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoFocus required />
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </div>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={submitting} style={{ width: '100%' }}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <p style={{ fontSize: '0.9rem', marginTop: 16 }}>
        Don't have an account? <Link to="/register">Create one</Link>
      </p>
    </AuthLayout>
  );
}
