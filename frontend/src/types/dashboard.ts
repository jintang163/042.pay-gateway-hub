import type { PayMethod, PayChannel } from './order';

export interface DashboardStats {
  todayOrderCount: number;
  todayOrderAmount: number;
  todaySuccessRate: number;
  activeMerchantCount: number;
  orderTrend: {
    date: string;
    orderCount: number;
    amount: number;
  }[];
  payMethodDistribution: {
    name: string;
    value: number;
  }[];
  payChannelDistribution: {
    name: string;
    value: number;
  }[];
  recentOrders: {
    orderNo: string;
    merchantName: string;
    amount: number;
    payMethod: PayMethod;
    status: string;
    createTime: string;
  }[];
}
