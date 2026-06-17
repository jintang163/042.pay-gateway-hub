import { request } from '@/utils/request';
import type { RiskEvent, RiskQueryParams, RiskEventListResult, RiskDashboardStats } from '@/types/risk';

export const riskApi = {
  list: (params: RiskQueryParams) => {
    return request.get<RiskEventListResult>('/api/risk/log/list', params);
  },

  detail: (id: string) => {
    return request.get<RiskEvent>(`/api/risk/log/${id}`);
  },

  check: (data: { merchantNo?: string; ip?: string; amount?: number; payMethod?: string }) => {
    return request.post<{ pass: boolean; riskLevel: string; reason?: string }>('/api/risk/check', data);
  },

  handle: (id: string, data: { handleRemark?: string; action: 'pass' | 'block' | 'review' }) => {
    return request.post<void>(`/api/risk/event/${id}/handle`, data);
  },

  ignore: (id: string, reason?: string) => {
    return request.post<void>(`/api/risk/event/${id}/ignore`, { reason });
  },

  getDashboardStats: () => {
    return request.get<RiskDashboardStats>('/api/risk/dashboard');
  },

  addToBlacklist: (data: { type: 'ip' | 'merchant' | 'user'; value: string; reason?: string }) => {
    return request.post<void>('/api/risk/blacklist/add', data);
  },

  removeFromBlacklist: (id: string) => {
    return request.delete<void>(`/api/risk/blacklist/${id}`);
  },

  getBlacklist: (params: RiskQueryParams) => {
    return request.get<RiskEventListResult>('/api/risk/blacklist/list', params);
  },

  getApiStats: (params?: { merchantNo?: string; startTime?: string; endTime?: string }) => {
    return request.get<unknown>('/api/api-stats/summary', params);
  },

  getApiCallTrend: (days: number = 7) => {
    return request.get<{ date: string; count: number }[]>('/api/api-stats/trend', { days });
  },
};
