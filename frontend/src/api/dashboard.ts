import { request } from '@/utils/request';
import type { DashboardStats } from '@/types/dashboard';

export const dashboardApi = {
  getStats: () => {
    return request.get<DashboardStats>('/pay/dashboard/stats');
  },

  getOrderTrend: (days: number = 7) => {
    return request.get<{ date: string; orderCount: number; amount: number }[]>('/pay/dashboard/order-trend', { days });
  },

  getPayMethodDistribution: () => {
    return request.get<{ name: string; value: number }[]>('/pay/dashboard/pay-method-distribution');
  },

  getRecentOrders: (limit: number = 10) => {
    return request.get<unknown[]>('/pay/dashboard/recent-orders', { limit });
  },
};
