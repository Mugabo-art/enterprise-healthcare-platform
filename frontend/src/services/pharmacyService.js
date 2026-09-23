import apiClient from './apiClient.js';

export async function listMedications(lowStock = false) {
  const { data } = await apiClient.get('/medications', { params: { lowStock } });
  return data;
}

export async function createMedication(payload) {
  const { data } = await apiClient.post('/medications', payload);
  return data;
}

export async function updateMedication(id, payload) {
  const { data } = await apiClient.put(`/medications/${id}`, payload);
  return data;
}

export async function restockMedication(id, quantity) {
  const { data } = await apiClient.post(`/medications/${id}/restock`, { quantity });
  return data;
}

export async function listPendingPrescriptions() {
  const { data } = await apiClient.get('/pharmacy/prescriptions');
  return data;
}

export async function dispensePrescription(prescriptionId, medicationId, quantity) {
  const { data } = await apiClient.post(`/prescriptions/${prescriptionId}/dispense`, { medicationId, quantity });
  return data;
}

export async function listDispensations(patientId) {
  const { data } = await apiClient.get(`/patients/${patientId}/dispensations`);
  return data;
}
