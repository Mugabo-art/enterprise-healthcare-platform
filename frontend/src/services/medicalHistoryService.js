import apiClient from './apiClient.js';

export async function listHistory(patientId) {
  const { data } = await apiClient.get(`/patients/${patientId}/medical-history`);
  return data;
}

export async function createHistoryEntry(patientId, payload) {
  const { data } = await apiClient.post(`/patients/${patientId}/medical-history`, payload);
  return data;
}
