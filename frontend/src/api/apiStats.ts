import { request } from '@/utils/request';
import type { ApiStats, ApiStatsQueryParams } from '@/types/apiStats';

export const apiStatsApi = {
  getOverview: (params: ApiStatsQueryParams) => {
    return request.get<ApiStats>('/api-stats/overview', params);
  },

  getDetail: (params: ApiStatsQueryParams) => {
    return request.get<{ list: unknown[]; total: number }>('/api-stats/detail', params);
  },

  getTopMerchants: (params: ApiStatsQueryParams) => {
    return request.get<{ merchantNo: string; merchantName: string; requestCount: number; successRate: number }[]>(
      '/api-stats/top-merchants',
      params
    );
  },

  getRequestTrend: (params: ApiStatsQueryParams) => {
    return request.get<{ time: string; successCount: number; failCount: number; avgResponseTime: number }[]>(
      '/api-stats/request-trend',
      params
    );
  },

  getApiDistribution: (params: ApiStatsQueryParams) => {
    return request.get<{ api: string; method: string; count: number; successRate: number; avgResponseTime: number }[]>(
      '/api-stats/api-distribution',
      params
    );
  },

  getErrorDistribution: (params: ApiStatsQueryParams) => {
    return request.get<{ code: string; message: string; count: number }[]>(
      '/api-stats/error-distribution',
      params
    );
  },

  getMerchantRanking: (params: ApiStatsQueryParams) => {
    return request.get<{ merchantId: string; merchantName: string; requestCount: number; successRate: number }[]>(
      '/api-stats/merchant-ranking',
      params
    );
  },

  export: (params: ApiStatsQueryParams) => {
    return request.download('/api-stats/export', params);
  },
};
