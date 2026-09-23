import apiClient from './apiClient.js';

export async function listLabRequests(patientId) {
  const { data } = await apiClient.get(`/patients/${patientId}/lab-requests`);
  return data;
}

export async function createLabRequest(patientId, visitId, payload) {
  const { data } = await apiClient.post(`/patients/${patientId}/visits/${visitId}/lab-requests`, payload);
  return data;
}

export async function updateLabRequestStatus(patientId, labRequestId, status) {
  const { data } = await apiClient.put(`/patients/${patientId}/lab-requests/${labRequestId}/status`, { status });
  return data;
}

export async function recordLabResult(patientId, labRequestId, resultData) {
  const { data } = await apiClient.post(`/patients/${patientId}/lab-requests/${labRequestId}/result`, { resultData });
  return data;
}

export async function getLabResult(patientId, labRequestId) {
  const { data } = await apiClient.get(`/patients/${patientId}/lab-requests/${labRequestId}/result`);
  return data;
}
