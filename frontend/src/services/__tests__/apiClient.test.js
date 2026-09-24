import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import axios from 'axios';
import apiClient, { configureSession, setAuthToken } from '../apiClient.js';

// A scripted adapter stands in for the network: a protected call with the old token is a 401,
// the same call with the new token succeeds.
function installAdapter() {
  const calls = [];
  apiClient.defaults.adapter = async (config) => {
    calls.push({ url: config.url, auth: config.headers.Authorization });
    if (config.headers.Authorization === 'Bearer new-access') {
      return { data: { ok: true }, status: 200, statusText: 'OK', headers: {}, config };
    }
    const error = new Error('Unauthorized');
    error.config = config;
    error.response = { status: 401, data: {}, config };
    throw error;
  };
  return calls;
}

describe('apiClient silent refresh', () => {
  let session;

  beforeEach(() => {
    setAuthToken('old-access');
    session = {
      getRefreshToken: vi.fn(() => 'refresh-1'),
      onTokens: vi.fn(),
      onLogout: vi.fn(),
    };
    configureSession(session);
  });

  afterEach(() => {
    configureSession(null);
    vi.restoreAllMocks();
  });

  it('refreshes once on 401 and replays the original request with the new token', async () => {
    const calls = installAdapter();
    vi.spyOn(axios, 'post').mockResolvedValue({ data: { accessToken: 'new-access', refreshToken: 'refresh-2' } });

    const response = await apiClient.get('/patients');

    expect(response.data).toEqual({ ok: true });
    expect(session.onTokens).toHaveBeenCalledWith({ accessToken: 'new-access', refreshToken: 'refresh-2' });
    expect(calls.map((c) => c.auth)).toEqual(['Bearer old-access', 'Bearer new-access']);
  });

  it('shares a single refresh between concurrent 401s (refresh tokens rotate)', async () => {
    installAdapter();
    const post = vi.spyOn(axios, 'post').mockResolvedValue({ data: { accessToken: 'new-access', refreshToken: 'refresh-2' } });

    await Promise.all([apiClient.get('/patients'), apiClient.get('/visits'), apiClient.get('/analytics/dashboard')]);

    expect(post).toHaveBeenCalledTimes(1);
  });

  it('logs the user out when the refresh itself fails', async () => {
    installAdapter();
    vi.spyOn(axios, 'post').mockRejectedValue(new Error('refresh rejected'));

    await expect(apiClient.get('/patients')).rejects.toBeTruthy();

    expect(session.onLogout).toHaveBeenCalledTimes(1);
  });

  it('does not try to refresh failed login attempts', async () => {
    installAdapter();
    const post = vi.spyOn(axios, 'post');

    await expect(apiClient.post('/auth/login', {})).rejects.toBeTruthy();

    expect(post).not.toHaveBeenCalled();
    expect(session.onLogout).not.toHaveBeenCalled();
  });
});
