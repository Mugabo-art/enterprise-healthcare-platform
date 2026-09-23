import apiClient from './apiClient.js';

export async function listPatients(search = '', page = 0, size = 10, sort = null) {
  const { data } = await apiClient.get('/patients', { params: { search, page, size, sort } });
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
