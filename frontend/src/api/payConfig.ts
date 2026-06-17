import { request } from '@/utils/request';
import type { PayConfig, PayConfigQueryParams, PayConfigListResult, PayConfigCreateRequest } from '@/types/payConfig';

export const payConfigApi = {
  list: (params: PayConfigQueryParams) => {
    return request.get<PayConfigListResult>('/pay/config/list', params);
  },

  detail: (id: string) => {
    return request.get<PayConfig>(`/pay/config/${id}`);
  },

  create: (data: PayConfigCreateRequest) => {
    return request.post<{ id: string; configId: string }>('/pay/config/create', data);
  },

  update: (id: string, data: Partial<PayConfigCreateRequest>) => {
    return request.put<void>(`/pay/config/${id}`, data);
  },

  remove: (id: string) => {
    return request.delete<void>(`/pay/config/${id}`);
  },

  enable: (id: string) => {
    return request.post<void>(`/pay/config/${id}/enable`);
  },

  disable: (id: string) => {
    return request.post<void>(`/pay/config/${id}/disable`);
  },

  test: (id: string) => {
    return request.post<{ success: boolean; message: string }>(`/pay/config/${id}/test`);
  },
};
