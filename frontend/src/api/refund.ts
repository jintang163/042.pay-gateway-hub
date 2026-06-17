import { request } from '@/utils/request';
import type { Refund, RefundQueryParams, RefundListResult, RefundApplyRequest } from '@/types/refund';

export const refundApi = {
  list: (params: RefundQueryParams) => {
    return request.get<RefundListResult>('/pay/refund/list', params);
  },

  detail: (id: string) => {
    return request.get<Refund>(`/pay/refund/${id}`);
  },

  apply: (data: RefundApplyRequest) => {
    return request.post<{ refundId: string; refundNo: string }>('/pay/refund/apply', data);
  },

  cancel: (id: string) => {
    return request.post<void>(`/pay/refund/${id}/cancel`);
  },

  retry: (id: string) => {
    return request.post<{ refundId: string; refundNo: string }>(`/pay/refund/${id}/retry`);
  },

  export: (params: Partial<RefundQueryParams>) => {
    return request.download('/pay/refund/export', params);
  },
};
