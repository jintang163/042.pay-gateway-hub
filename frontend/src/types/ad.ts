export type AdPosition = 'PAY_SUCCESS';

export interface MerchantAd {
  id?: number;
  adCode: string;
  merchantNo: string;
  adTitle: string;
  adDescription?: string;
  adImageUrl?: string;
  targetUrl: string;
  position: AdPosition | string;
  positionDesc?: string;
  cpcPrice: number;
  sortOrder: number;
  status: number;
  statusDesc?: string;
  startTime?: string;
  endTime?: string;
  dailyBudget?: number;
  clickCount?: number;
  impressionCount?: number;
  totalCost?: number;
  ctr?: number;
  operatorId?: string;
  operatorName?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface MerchantAdSaveRequest {
  id?: number;
  merchantNo?: string;
  adTitle: string;
  adDescription?: string;
  adImageUrl?: string;
  targetUrl: string;
  position: AdPosition | string;
  cpcPrice: number;
  sortOrder: number;
  status: number;
  startTime?: string;
  endTime?: string;
  dailyBudget?: number;
  remark?: string;
}

export interface AdDisplayVO {
  adCode: string;
  adTitle: string;
  adDescription?: string;
  adImageUrl?: string;
  targetUrl: string;
  position: AdPosition | string;
  cpcPrice?: number;
}

export interface AdClickReportRequest {
  adCode: string;
  orderNo?: string;
  payAmount?: number;
  position?: AdPosition | string;
  deviceId?: string;
  refererUrl?: string;
}

export interface AdClickReportResult {
  clickNo: string;
  adCode: string;
  targetUrl: string;
  cpcPrice: number;
  costAmount: number;
  valid: boolean;
  invalidReason?: string;
}

export interface AdDailyStatsVO {
  statsDate: string;
  impressionCount: number;
  clickCount: number;
  validClickCount: number;
  invalidClickCount: number;
  totalCost: number;
  ctr: number;
  avgCpc: number;
  orderCount: number;
  orderAmount: number;
}

export interface AdItemStatsVO {
  adCode: string;
  adTitle: string;
  position: AdPosition | string;
  positionDesc?: string;
  cpcPrice?: number;
  impressionCount: number;
  clickCount: number;
  validClickCount: number;
  invalidClickCount: number;
  totalCost: number;
  ctr: number;
  avgCpc: number;
  orderCount: number;
  orderAmount: number;
  status?: number;
  statusDesc?: string;
}

export interface AdStatsOverviewVO {
  merchantNo?: string;
  startDate: string;
  endDate: string;
  totalImpression: number;
  totalClick: number;
  totalValidClick: number;
  totalInvalidClick: number;
  totalCost: number;
  overallCtr: number;
  overallAvgCpc: number;
  totalOrder: number;
  totalOrderAmount: number;
  dailyStats: AdDailyStatsVO[];
  adStats: AdItemStatsVO[];
}
