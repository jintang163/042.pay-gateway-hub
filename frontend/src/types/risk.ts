import type { PageParams, PageResult } from './common';

export type RiskLevel = 'low' | 'medium' | 'high' | 'critical';
export type RiskStatus = 'pending' | 'handled' | 'ignored';
export type RiskType = 'fraud' | 'abnormal' | 'limit_exceed' | 'blacklist' | 'other';

export interface RiskEvent {
  id: string;
  eventId: string;
  merchantId?: string;
  merchantName?: string;
  orderId?: string;
  orderNo?: string;
  type: RiskType;
  level: RiskLevel;
  title: string;
  description?: string;
  evidence?: Record<string, unknown>;
  status: RiskStatus;
  handler?: string;
  handleTime?: string;
  handleRemark?: string;
  createTime: string;
  updateTime?: string;
}

export interface RiskQueryParams extends PageParams {
  eventId?: string;
  merchantId?: string;
  orderNo?: string;
  type?: RiskType;
  level?: RiskLevel;
  status?: RiskStatus;
  startTime?: string;
  endTime?: string;
}

export type RiskEventListResult = PageResult<RiskEvent>;

export interface RiskDashboardStats {
  totalEvents: number;
  pendingEvents: number;
  highRiskEvents: number;
  todayEvents: number;
  eventTrend: { date: string; count: number }[];
  riskLevelDistribution: { name: string; value: number }[];
  riskTypeDistribution: { name: string; value: number }[];
}
