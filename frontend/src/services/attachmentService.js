import apiClient from './apiClient.js';

export async function listAttachments(patientId) {
  const { data } = await apiClient.get(`/patients/${patientId}/attachments`);
  return data;
}

export async function uploadAttachment(patientId, file) {
  const formData = new FormData();
  formData.append('file', file);
  const { data } = await apiClient.post(`/patients/${patientId}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}

// Downloads require the auth header, so a plain <a href> won't work — fetch the
// bytes through apiClient (which attaches the bearer token) and save via a blob URL.
export async function downloadAttachment(patientId, attachmentId, fileName) {
  const response = await apiClient.get(`/patients/${patientId}/attachments/${attachmentId}/download`, {
    responseType: 'blob',
  });
  const url = window.URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
