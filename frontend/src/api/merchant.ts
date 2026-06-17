import { request } from '@/utils/request';
import type { Merchant, MerchantQueryParams, MerchantListResult, MerchantApplyRequest } from '@/types/merchant';

export const merchantApi = {
  list: (params: MerchantQueryParams) => {
    return request.get<MerchantListResult>('/merchant/list', params);
  },

  detail: (id: string) => {
    return request.get<Merchant>(`/merchant/${id}`);
  },

  apply: (data: MerchantApplyRequest) => {
    return request.post<{ merchantId: string; merchantNo: string }>('/merchant/apply', data);
  },

  approve: (id: string) => {
    return request.post<void>(`/merchant/${id}/approve`);
  },

  reject: (id: string, reason: string) => {
    return request.post<void>(`/merchant/${id}/reject`, { reason });
  },

  suspend: (id: string, reason?: string) => {
    return request.post<void>(`/merchant/${id}/suspend`, { reason });
  },

  resume: (id: string) => {
    return request.post<void>(`/merchant/${id}/resume`);
  },

  terminate: (id: string, reason?: string) => {
    return request.post<void>(`/merchant/${id}/terminate`, { reason });
  },

  update: (id: string, data: Partial<Merchant>) => {
    return request.put<void>(`/merchant/${id}`, data);
  },
};
