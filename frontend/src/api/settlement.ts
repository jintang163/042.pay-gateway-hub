import { request } from '@/utils/request';
import type {
  Settlement,
  SettlementQueryParams,
  SettlementListResult,
  SplitRule,
  SplitRuleQueryParams,
  SplitRuleListResult,
  SplitRuleCreateRequest,
} from '@/types/settlement';

export const settlementApi = {
  list: (params: SettlementQueryParams) => {
    return request.get<SettlementListResult>('/settlement/list', params);
  },

  detail: (id: string) => {
    return request.get<Settlement>(`/settlement/${id}`);
  },

  create: (data: { merchantId: string; periodStart: string; periodEnd: string }) => {
    return request.post<{ settlementId: string; settlementNo: string }>('/settlement/create', data);
  },

  retry: (id: string) => {
    return request.post<{ settlementId: string; settlementNo: string }>(`/settlement/${id}/retry`);
  },

  export: (params: Partial<SettlementQueryParams>) => {
    return request.download('/settlement/export', params);
  },

  getSummary: (merchantId?: string) => {
    return request.get<{
      totalAmount: number;
      totalFee: number;
      settlementAmount: number;
      orderCount: number;
    }>('/settlement/summary', { merchantId });
  },
};

export const splitRuleApi = {
  list: (params: SplitRuleQueryParams) => {
    return request.get<SplitRuleListResult>('/split-rule/list', params);
  },

  detail: (id: string) => {
    return request.get<SplitRule>(`/split-rule/${id}`);
  },

  create: (data: SplitRuleCreateRequest) => {
    return request.post<{ id: string; ruleId: string }>('/split-rule/create', data);
  },

  update: (id: string, data: Partial<SplitRuleCreateRequest>) => {
    return request.put<void>(`/split-rule/${id}`, data);
  },

  remove: (id: string) => {
    return request.delete<void>(`/split-rule/${id}`);
  },

  enable: (id: string) => {
    return request.post<void>(`/split-rule/${id}/enable`);
  },

  disable: (id: string) => {
    return request.post<void>(`/split-rule/${id}/disable`);
  },
};

export const reconcileApi = {
  list: (params: { pageNum: number; pageSize: number; merchantId?: string; date?: string; status?: string }) => {
    return request.get<{ list: unknown[]; total: number }>('/reconcile/list', params);
  },

  detail: (id: string) => {
    return request.get<unknown>(`/reconcile/${id}`);
  },

  create: (data: { merchantId: string; date: string }) => {
    return request.post<{ id: string; reconcileNo: string }>('/reconcile/create', data);
  },

  download: (id: string) => {
    return request.download(`/reconcile/${id}/download`);
  },
};
