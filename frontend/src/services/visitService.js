import apiClient from './apiClient.js';

export async function listVisits(patientId) {
  const { data } = await apiClient.get(`/patients/${patientId}/visits`);
  return data;
}

export async function createVisit(patientId, payload) {
  const { data } = await apiClient.post(`/patients/${patientId}/visits`, payload);
  return data;
}

export async function updateVisit(patientId, visitId, payload) {
  const { data } = await apiClient.put(`/patients/${patientId}/visits/${visitId}`, payload);
  return data;
}
