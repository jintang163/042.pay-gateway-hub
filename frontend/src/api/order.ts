import { request } from '@/utils/request';
import type { Order, OrderQueryParams, OrderListResult, OrderDetail, OrderAttributionVO } from '@/types/order';

export const orderApi = {
  list: (params: OrderQueryParams) => {
    return request.get<OrderListResult>('/api/pay/order/list', params);
  },

  detail: (orderNo: string) => {
    return request.get<OrderDetail>(`/api/pay/order/${orderNo}`);
  },

  attribution: (orderNo: string) => {
    return request.get<OrderAttributionVO>(`/api/pay/order/${orderNo}/attribution`);
  },

  query: (data: { orderNo?: string; outTradeNo?: string }) => {
    return request.post<OrderDetail>('/api/pay/query', data);
  },

  unifiedOrder: (data: Partial<Order>) => {
    return request.post<{ id: string; orderNo: string; payUrl?: string }>('/api/pay/unifiedorder', data);
  },

  create: (data: Partial<Order>) => {
    return request.post<{ id: string; orderNo: string; payUrl?: string }>('/api/pay/unifiedorder', data);
  },

  close: (id: string) => {
    return request.post<void>(`/api/pay/order/${id}/close`);
  },

  refund: (id: string, data: { refundAmount: number; reason: string }) => {
    return request.post<{ refundId: string; refundNo: string }>('/api/pay/refund/apply', { orderId: id, ...data });
  },

  export: (params: Partial<OrderQueryParams>) => {
    return request.download('/api/pay/order/export', params);
  },

  getStatusStats: () => {
    return request.get<{ status: string; count: number }[]>('/api/pay/order/status-stats');
  },
};
