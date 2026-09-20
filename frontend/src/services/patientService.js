import apiClient from './apiClient.js';

export async function listPatients(search = '') {
  const { data } = await apiClient.get('/patients', { params: { search } });
  return data;
}

export async function getPatient(id) {
  const { data } = await apiClient.get(`/patients/${id}`);
  return data;
}

export async function createPatient(payload) {
  const { data } = await apiClient.post('/patients', payload);
  return data;
}
