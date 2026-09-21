import apiClient from './apiClient.js';

export async function getDashboardAnalytics() {
  const { data } = await apiClient.get('/analytics/dashboard');
  return data;
}
