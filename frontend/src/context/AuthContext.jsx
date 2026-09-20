import { createContext, useContext, useState, useCallback } from 'react';
import { setAuthToken } from '../services/apiClient.js';
import * as authService from '../services/authService.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [accessToken, setAccessToken] = useState(null);
  const [refreshToken, setRefreshToken] = useState(null);

  const applyTokens = useCallback((tokens) => {
    setAccessToken(tokens.accessToken);
    setRefreshToken(tokens.refreshToken);
    setAuthToken(tokens.accessToken);
  }, []);

  const login = useCallback(
    async (email, password) => {
      const result = await authService.login(email, password);
      if (result.mfaRequired) {
        return result; // caller routes to an MFA prompt
      }
      applyTokens(result);
      return result;
    },
    [applyTokens]
  );

  const completeMfa = useCallback(
    async (challengeId, code) => {
      const tokens = await authService.verifyMfa(challengeId, code);
      applyTokens(tokens);
      return tokens;
    },
    [applyTokens]
  );

  const logout = useCallback(async () => {
    try {
      await authService.logout();
    } finally {
      setAccessToken(null);
      setRefreshToken(null);
      setAuthToken(null);
    }
  }, []);

  return (
    <AuthContext.Provider value={{ accessToken, refreshToken, login, completeMfa, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
