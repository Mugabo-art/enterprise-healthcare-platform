import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, act, waitFor } from '@testing-library/react';
import { AuthProvider, useAuth } from '../AuthContext.jsx';

const authService = vi.hoisted(() => ({
  login: vi.fn(),
  verifyMfa: vi.fn(),
  logout: vi.fn(),
  me: vi.fn(),
}));
vi.mock('../../services/authService.js', () => authService);

let latest;
function Probe() {
  latest = useAuth();
  return <div data-testid="state">{latest.loading ? 'loading' : latest.user ? latest.user.email : 'anonymous'}</div>;
}

const tokens = { accessToken: 'a1', refreshToken: 'r1', expiresIn: 900 };
const doctor = { id: 'u1', email: 'doc@hospital.test', role: 'DOCTOR' };

async function renderAnonymous() {
  render(
    <AuthProvider>
      <Probe />
    </AuthProvider>
  );
  await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('anonymous'));
}

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear();
    Object.values(authService).forEach((fn) => fn.mockReset());
  });

  it('starts anonymous when no session is stored', async () => {
    await renderAnonymous();
  });

  it('logs in, stores the tokens and exposes the user', async () => {
    authService.login.mockResolvedValue(tokens);
    authService.me.mockResolvedValue(doctor);
    await renderAnonymous();

    await act(async () => {
      await latest.login('doc@hospital.test', 'Password123!');
    });

    expect(screen.getByTestId('state')).toHaveTextContent('doc@hospital.test');
    expect(localStorage.getItem('medicore_access_token')).toBe('a1');
  });

  it('returns the MFA challenge without storing any tokens', async () => {
    authService.login.mockResolvedValue({ mfaRequired: true, challengeId: 'signed-challenge' });
    await renderAnonymous();

    let result;
    await act(async () => {
      result = await latest.login('doc@hospital.test', 'Password123!');
    });

    expect(result.challengeId).toBe('signed-challenge');
    expect(localStorage.getItem('medicore_access_token')).toBeNull();
  });

  it('rehydrates a stored session and drops it if the server rejects the token', async () => {
    localStorage.setItem('medicore_access_token', 'stale');
    localStorage.setItem('medicore_refresh_token', 'stale-r');
    authService.me.mockRejectedValue(new Error('401'));

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>
    );

    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('anonymous'));
    expect(localStorage.getItem('medicore_access_token')).toBeNull();
  });

  it('clears the local session on logout even if the server call fails', async () => {
    authService.login.mockResolvedValue(tokens);
    authService.me.mockResolvedValue(doctor);
    authService.logout.mockRejectedValue(new Error('network'));
    await renderAnonymous();
    await act(async () => {
      await latest.login('doc@hospital.test', 'Password123!');
    });

    await act(async () => {
      await latest.logout().catch(() => {});
    });

    expect(screen.getByTestId('state')).toHaveTextContent('anonymous');
    expect(localStorage.getItem('medicore_refresh_token')).toBeNull();
  });
});
