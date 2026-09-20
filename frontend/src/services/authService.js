import apiClient from './apiClient.js';

export async function register(email, password, role) {
  const { data } = await apiClient.post('/auth/register', { email, password, role });
  return data;
}

export async function login(email, password) {
  const { data } = await apiClient.post('/auth/login', { email, password });
  return data; // either {accessToken, refreshToken, expiresIn} or {mfaRequired, challengeId}
}

export async function verifyMfa(challengeId, code) {
  const { data } = await apiClient.post('/auth/mfa/verify', { challengeId, code });
  return data;
}

export async function refreshTokens(refreshToken) {
  const { data } = await apiClient.post('/auth/refresh', { refreshToken });
  return data;
}

export async function logout() {
  await apiClient.post('/auth/logout');
}
