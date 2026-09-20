import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { setAuthToken } from '../services/apiClient.js';
import * as authService from '../services/authService.js';

const AuthContext = createContext(null);
const ACCESS_TOKEN_KEY = 'medicore_access_token';
const REFRESH_TOKEN_KEY = 'medicore_refresh_token';

export function AuthProvider({ children }) {
  const [accessToken, setAccessToken] = useState(null);
  const [refreshToken, setRefreshToken] = useState(null);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const applyTokens = useCallback((tokens) => {
    setAccessToken(tokens.accessToken);
    setRefreshToken(tokens.refreshToken);
    setAuthToken(tokens.accessToken);
    localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
  }, []);

  const clearSession = useCallback(() => {
    setAccessToken(null);
    setRefreshToken(null);
    setUser(null);
    setAuthToken(null);
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  }, []);

  // Rehydrate the session on load so a page refresh doesn't log the user out.
  useEffect(() => {
    const storedAccess = localStorage.getItem(ACCESS_TOKEN_KEY);
    const storedRefresh = localStorage.getItem(REFRESH_TOKEN_KEY);
    if (!storedAccess) {
      setLoading(false);
      return;
    }
    setAccessToken(storedAccess);
    setRefreshToken(storedRefresh);
    setAuthToken(storedAccess);
    authService
      .me()
      .then(setUser)
      .catch(clearSession)
      .finally(() => setLoading(false));
  }, [clearSession]);

  const login = useCallback(
    async (email, password) => {
      const result = await authService.login(email, password);
      if (result.mfaRequired) {
        return result; // caller routes to an MFA prompt
      }
      applyTokens(result);
      setUser(await authService.me());
      return result;
    },
    [applyTokens]
  );

  const completeMfa = useCallback(
    async (challengeId, code) => {
      const tokens = await authService.verifyMfa(challengeId, code);
      applyTokens(tokens);
      setUser(await authService.me());
      return tokens;
    },
    [applyTokens]
  );

  const logout = useCallback(async () => {
    try {
      await authService.logout();
    } finally {
      clearSession();
    }
  }, [clearSession]);

  return (
    <AuthContext.Provider value={{ accessToken, refreshToken, user, loading, login, completeMfa, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
