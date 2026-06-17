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
    return request.get<SettlementListResult>('/api/settlement/settlement/list', params);
  },

  detail: (settlementNo: string) => {
    return request.get<Settlement>(`/api/settlement/settlement/${settlementNo}`);
  },

  generate: (data: { merchantId: string; periodStart: string; periodEnd: string }) => {
    return request.post<{ settlementId: string; settlementNo: string }>('/api/settlement/settlement/generate', data);
  },

  create: (data: { merchantId: string; periodStart: string; periodEnd: string }) => {
    return request.post<{ settlementId: string; settlementNo: string }>('/api/settlement/settlement/generate', data);
  },

  confirm: (id: string) => {
    return request.post<void>(`/api/settlement/settlement/confirm/${id}`);
  },

  retry: (id: string) => {
    return request.post<{ settlementId: string; settlementNo: string }>(`/api/settlement/${id}/retry`);
  },

  export: (params: Partial<SettlementQueryParams>) => {
    return request.download('/api/settlement/export', params);
  },

  getSummary: (merchantId?: string) => {
    return request.get<{
      totalAmount: number;
      totalFee: number;
      settlementAmount: number;
      orderCount: number;
    }>('/api/settlement/summary', { merchantId });
  },
};

export const splitRuleApi = {
  list: (params: SplitRuleQueryParams) => {
    return request.get<SplitRuleListResult>('/api/split-rule/list', params);
  },

  detail: (id: string) => {
    return request.get<SplitRule>(`/api/split-rule/${id}`);
  },

  save: (data: SplitRuleCreateRequest) => {
    return request.post<{ id: string; ruleId: string }>('/api/split-rule/save', data);
  },

  create: (data: SplitRuleCreateRequest) => {
    return request.post<{ id: string; ruleId: string }>('/api/split-rule/save', data);
  },

  update: (id: string, data: Partial<SplitRuleCreateRequest>) => {
    return request.put<void>(`/api/split-rule/${id}`, data);
  },

  remove: (id: string) => {
    return request.post<void>(`/api/split-rule/delete/${id}`);
  },

  delete: (id: string) => {
    return request.post<void>(`/api/split-rule/delete/${id}`);
  },

  enable: (id: string) => {
    return request.post<void>(`/api/split-rule/${id}/enable`);
  },

  disable: (id: string) => {
    return request.post<void>(`/api/split-rule/${id}/disable`);
  },
};

export const reconcileApi = {
  list: (params: { pageNum: number; pageSize: number; merchantId?: string; date?: string; status?: string }) => {
    return request.get<{ list: unknown[]; total: number }>('/api/reconcile/list', params);
  },

  detail: (id: string) => {
    return request.get<unknown>(`/api/reconcile/${id}`);
  },

  generate: (data: { merchantId: string; date: string }) => {
    return request.post<{ id: string; reconcileNo: string }>('/api/reconcile/generate', data);
  },

  create: (data: { merchantId: string; date: string }) => {
    return request.post<{ id: string; reconcileNo: string }>('/api/reconcile/generate', data);
  },

  download: (id: string) => {
    return request.download(`/api/reconcile/${id}/download`);
  },
};
