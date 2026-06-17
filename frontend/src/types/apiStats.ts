export interface ApiStats {
  totalRequestCount: number;
  totalSuccessCount: number;
  totalFailCount: number;
  totalPv: number;
  totalQps: number;
  avgResponseTime: number;
  maxResponseTime: number;
  minResponseTime: number;
  successRate: number;
  requestTrend: {
    time: string;
    successCount: number;
    failCount: number;
    avgResponseTime: number;
  }[];
  apiDistribution: {
    api: string;
    method: string;
    count: number;
    successRate: number;
    avgResponseTime: number;
  }[];
  errorDistribution: {
    code: string;
    message: string;
    count: number;
  }[];
  merchantTop10: {
    merchantId: string;
    merchantName: string;
    requestCount: number;
    successRate: number;
  }[];
}

export interface ApiStatsQueryParams {
  startTime: string;
  endTime: string;
  merchantId?: string;
  api?: string;
  interval?: 'hour' | 'day' | 'week' | 'month';
}
