import { request } from '@/utils/request';
import type { RiskEvent, RiskQueryParams, RiskEventListResult, RiskDashboardStats } from '@/types/risk';

export const riskApi = {
  list: (params: RiskQueryParams) => {
    return request.get<RiskEventListResult>('/risk/event/list', params);
  },

  detail: (id: string) => {
    return request.get<RiskEvent>(`/risk/event/${id}`);
  },

  handle: (id: string, data: { handleRemark?: string; action: 'pass' | 'block' | 'review' }) => {
    return request.post<void>(`/risk/event/${id}/handle`, data);
  },

  ignore: (id: string, reason?: string) => {
    return request.post<void>(`/risk/event/${id}/ignore`, { reason });
  },

  getDashboardStats: () => {
    return request.get<RiskDashboardStats>('/risk/dashboard');
  },

  addToBlacklist: (data: { type: 'ip' | 'merchant' | 'user'; value: string; reason?: string }) => {
    return request.post<void>('/risk/blacklist/add', data);
  },

  removeFromBlacklist: (id: string) => {
    return request.delete<void>(`/risk/blacklist/${id}`);
  },

  getBlacklist: (params: RiskQueryParams) => {
    return request.get<RiskEventListResult>('/risk/blacklist/list', params);
  },
};
