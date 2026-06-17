import { request } from '@/utils/request';
import type { Order, OrderQueryParams, OrderListResult, OrderDetail } from '@/types/order';

export const orderApi = {
  list: (params: OrderQueryParams) => {
    return request.get<OrderListResult>('/pay/order/list', params);
  },

  detail: (id: string) => {
    return request.get<OrderDetail>(`/pay/order/${id}`);
  },

  detailByOrderNo: (orderNo: string) => {
    return request.get<OrderDetail>('/pay/order/detail', { orderNo });
  },

  create: (data: Partial<Order>) => {
    return request.post<{ id: string; orderNo: string; payUrl?: string }>('/pay/order/create', data);
  },

  close: (id: string) => {
    return request.post<void>(`/pay/order/${id}/close`);
  },

  refund: (id: string, data: { refundAmount: number; reason: string }) => {
    return request.post<{ refundId: string; refundNo: string }>(`/pay/refund/apply`, { orderId: id, ...data });
  },

  export: (params: Partial<OrderQueryParams>) => {
    return request.download('/pay/order/export', params);
  },

  getStatusStats: () => {
    return request.get<{ status: string; count: number }[]>('/pay/order/status-stats');
  },
};
