import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';
const apiClient = axios.create({ baseURL });

// Attaches the access token to every request.
export function setAuthToken(token) {
  if (token) {
    apiClient.defaults.headers.common.Authorization = `Bearer ${token}`;
  } else {
    delete apiClient.defaults.headers.common.Authorization;
  }
}

// --- Silent token refresh -------------------------------------------------------------------
// Access tokens live 15 minutes. On a 401 from a protected endpoint we exchange the refresh token
// once (shared by every request that fails at the same moment, because refresh tokens rotate and a
// second concurrent use would look like token theft), then replay the original request.
let session = null; // { getRefreshToken, onTokens, onLogout }
let refreshInFlight = null;

export function configureSession(handlers) {
  session = handlers;
}

async function refreshOnce() {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      const refreshToken = session.getRefreshToken();
      if (!refreshToken) throw new Error('no refresh token');
      // Bare axios call: must not pass through this interceptor again.
      const { data } = await axios.post(`${baseURL}/auth/refresh`, { refreshToken });
      session.onTokens(data);
      return data.accessToken;
    })().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

const AUTH_PATHS = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/mfa/verify'];

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const isAuthCall = AUTH_PATHS.some((p) => original?.url?.includes(p));
    if (error.response?.status !== 401 || !session || !original || original._retried || isAuthCall) {
      return Promise.reject(error);
    }
    original._retried = true;
    try {
      const accessToken = await refreshOnce();
      original.headers.Authorization = `Bearer ${accessToken}`;
      return apiClient(original);
    } catch {
      session.onLogout();
      return Promise.reject(error);
    }
  }
);

export default apiClient;
