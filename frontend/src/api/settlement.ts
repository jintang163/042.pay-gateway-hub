import { request } from '@/utils/request';
import type {
  Settlement,
  SettlementQueryParams,
  SettlementListResult,
  SplitRule,
  SplitRuleQueryParams,
  SplitRuleListResult,
  SplitRuleSaveRequest,
  SplitDetail,
  SplitDetailQueryParams,
  SplitDetailListResult,
  SplitCalculateRequest,
} from '@/types/settlement';

export const settlementApi = {
  list: (params: SettlementQueryParams) => {
    return request.get<SettlementListResult>('/api/settlement/list', params);
  },

  detail: (id: number) => {
    return request.get<Settlement>(`/api/settlement/${id}`);
  },

  generate: (data: { settleDate: string }) => {
    return request.post<{ id: number; settlementNo: string }>('/api/settlement/generate', data);
  },

  confirm: (id: number) => {
    return request.post<void>(`/api/settlement/${id}/confirm`);
  },

  retry: (id: number) => {
    return request.post<void>(`/api/settlement/${id}/retry`);
  },

  retryAll: () => {
    return request.post<void>('/api/settlement/retry-all');
  },

  details: (id: number) => {
    return request.get<SplitDetailListResult>(`/api/settlement/${id}/details`);
  },
};

export const splitRuleApi = {
  list: (params: SplitRuleQueryParams) => {
    return request.get<SplitRuleListResult>('/api/split-rule/list', params);
  },

  detail: (id: number) => {
    return request.get<SplitRule>(`/api/split-rule/${id}`);
  },

  save: (data: SplitRuleSaveRequest) => {
    return request.post<{ id: number; ruleNo: string }>('/api/split-rule/save', data);
  },

  delete: (id: number) => {
    return request.post<void>(`/api/split-rule/delete/${id}`);
  },

  toggle: (id: number) => {
    return request.post<void>(`/api/split-rule/${id}/toggle`);
  },
};

export const splitDetailApi = {
  list: (params: SplitDetailQueryParams) => {
    return request.get<SplitDetailListResult>('/api/split-detail/list', params);
  },

  getByOrderNo: (orderNo: string) => {
    return request.get<SplitDetail[]>(`/api/split-detail/order/${orderNo}`);
  },

  calculate: (data: SplitCalculateRequest) => {
    return request.post<SplitDetail[]>('/api/split-detail/calculate', data);
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
