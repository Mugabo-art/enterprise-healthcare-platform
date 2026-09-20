import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [mfaChallenge, setMfaChallenge] = useState(null);
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const { login, completeMfa } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const justRegistered = location.state?.justRegistered;

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      const result = await login(email, password);
      if (result.mfaRequired) {
        setMfaChallenge(result.challengeId);
      } else {
        navigate('/dashboard');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed');
    }
  }

  async function handleMfaSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      await completeMfa(mfaChallenge, code);
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid code');
    }
  }

  if (mfaChallenge) {
    return (
      <div className="card">
        <h2>Enter your MFA code</h2>
        <form onSubmit={handleMfaSubmit}>
          <div className="field">
            <label htmlFor="code">6-digit code</label>
            <input id="code" value={code} onChange={(e) => setCode(e.target.value)} autoFocus />
          </div>
          {error && <p className="error">{error}</p>}
          <button type="submit">Verify</button>
        </form>
      </div>
    );
  }

  return (
    <div className="card" style={{ maxWidth: 420, margin: '64px auto' }}>
      <h2>Log in to MediCore</h2>
      {justRegistered && (
        <p style={{ color: '#0F7A38', background: '#ECFDF3', padding: '10px 14px', borderRadius: 8, fontSize: '0.9rem' }}>
          Account created — log in with your new credentials.
        </p>
      )}
      <form onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoFocus />
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>
        {error && <p className="error">{error}</p>}
        <button type="submit" style={{ width: '100%' }}>Sign in</button>
      </form>
      <p style={{ fontSize: '0.9rem', marginTop: 16 }}>
        Don't have an account? <Link to="/register">Create one</Link>
      </p>
    </div>
  );
}
