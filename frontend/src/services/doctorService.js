import apiClient from './apiClient.js';

export async function getSchedule(doctorId) {
  const { data } = await apiClient.get(`/doctors/${doctorId}/visits`);
  return data;
}

export async function listPrescriptions(patientId) {
  const { data } = await apiClient.get(`/patients/${patientId}/prescriptions`);
  return data;
}

export async function createPrescription(patientId, visitId, payload) {
  const { data } = await apiClient.post(`/patients/${patientId}/visits/${visitId}/prescriptions`, payload);
  return data;
}
