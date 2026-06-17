import { request } from '@/utils/request';
import type { Refund, RefundQueryParams, RefundListResult, RefundApplyRequest } from '@/types/refund';

export const refundApi = {
  list: (params: RefundQueryParams) => {
    return request.get<RefundListResult>('/api/pay/refund/list', params);
  },

  detail: (id: string) => {
    return request.get<Refund>(`/api/pay/refund/${id}`);
  },

  apply: (data: RefundApplyRequest) => {
    return request.post<{ refundId: string; refundNo: string }>('/api/pay/refund/apply', data);
  },

  query: (data: { refundNo?: string; outRefundNo?: string; orderNo?: string }) => {
    return request.post<Refund>('/api/pay/refund/query', data);
  },

  cancel: (id: string) => {
    return request.post<void>(`/api/pay/refund/${id}/cancel`);
  },

  retry: (refundNo: string) => {
    return request.post<{ refundId: string; refundNo: string }>(`/api/pay/refund/retry/${refundNo}`);
  },

  retryRefund: (refundNo: string) => {
    return request.post<void>(`/api/pay/refund/retry/${refundNo}`);
  },

  export: (params: Partial<RefundQueryParams>) => {
    return request.download('/api/pay/refund/export', params);
  },
};
