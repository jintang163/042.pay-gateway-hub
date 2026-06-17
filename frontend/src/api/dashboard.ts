import { request } from '@/utils/request';
import type { DashboardStats } from '@/types/dashboard';

export const dashboardApi = {
  getOverview: () => {
    return request.get<DashboardStats>('/api/pay/dashboard/overview');
  },

  getStats: () => {
    return request.get<DashboardStats>('/api/pay/dashboard/overview');
  },

  getSuccessRateTrend: (days: number = 7) => {
    return request.get<{ date: string; totalCount: number; successCount: number; successRate: number }[]>('/api/pay/dashboard/success-rate', { days });
  },

  getOrderTrend: (days: number = 7) => {
    return request.get<{ date: string; orderCount: number; amount: number }[]>('/api/pay/dashboard/order-trend', { days });
  },

  getChannelDistribution: () => {
    return request.get<{ channelCode: string; channelName: string; orderCount: number; amount: number; percentage: number }[]>('/api/pay/dashboard/channel-distribution');
  },

  getPayMethodDistribution: () => {
    return request.get<{ name: string; value: number }[]>('/api/pay/dashboard/channel-distribution');
  },

  getRecentOrders: (limit: number = 10) => {
    return request.get<unknown[]>('/api/pay/dashboard/recent-orders', { limit });
  },
};
