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
  Reconcile,
  ReconcileQueryParams,
  ReconcileListResult,
  ReconcileDetail,
  ReconcileDetailQueryParams,
  ReconcileDetailListResult,
  ReconcileSummary,
  ErrorOrder,
  ErrorOrderQueryParams,
  ErrorOrderListResult,
  ErrorOrderApplyRequest,
  ErrorOrderAuditRequest,
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
  list: (params: ReconcileQueryParams) => {
    return request.get<ReconcileListResult>('/api/settlement/reconcile/list', {
      current: params.pageNum,
      size: params.pageSize,
      payChannel: params.payChannel,
      reconcileDate: params.reconcileDate,
      reconcileStatus: params.reconcileStatus,
    });
  },

  detail: (reconcileNo: string) => {
    return request.get<Reconcile>(`/api/settlement/reconcile/${reconcileNo}`);
  },

  execute: (data: { payChannel: string; reconcileDate: string }) => {
    return request.post<void>('/api/settlement/reconcile/execute', null, {
      params: data,
    });
  },

  summary: (params: { reconcileNo?: string; reconcileDate?: string; payChannel?: string }) => {
    return request.get<ReconcileSummary>('/api/settlement/reconcile/summary', params);
  },

  export: (params: Record<string, unknown>) => {
    return request.download('/api/settlement/reconcile/export', params);
  },
};

export const reconcileDetailApi = {
  list: (params: ReconcileDetailQueryParams) => {
    return request.get<ReconcileDetailListResult>('/api/settlement/reconcile-detail/list', {
      current: params.pageNum,
      size: params.pageSize,
      reconcileNo: params.reconcileNo,
      reconcileDate: params.reconcileDate,
      payChannel: params.payChannel,
      diffType: params.diffType,
      handleStatus: params.handleStatus,
      orderNo: params.orderNo,
      merchantNo: params.merchantNo,
      channelTradeNo: params.channelTradeNo,
    });
  },

  listByReconcile: (reconcileNo: string) => {
    return request.get<ReconcileDetail[]>(`/api/settlement/reconcile-detail/list-by-reconcile/${reconcileNo}`);
  },

  handle: (data: { detailId: number; handleStatus: number; handleRemark?: string; handleUserId?: string; handleUserName?: string }) => {
    return request.post<void>('/api/settlement/reconcile-detail/handle', null, { params: data });
  },

  ignore: (detailId: number, handleRemark?: string, handleUserId?: string, handleUserName?: string) => {
    return request.post<void>(`/api/settlement/reconcile-detail/ignore/${detailId}`, null, {
      params: { handleRemark, handleUserId, handleUserName },
    });
  },
};

export const errorOrderApi = {
  list: (params: ErrorOrderQueryParams) => {
    return request.get<ErrorOrderListResult>('/api/settlement/error-order/list', {
      current: params.pageNum,
      size: params.pageSize,
      errorNo: params.errorNo,
      reconcileNo: params.reconcileNo,
      payChannel: params.payChannel,
      errorType: params.errorType,
      errorStatus: params.errorStatus,
      auditStatus: params.auditStatus,
      handleType: params.handleType,
      orderNo: params.orderNo,
      merchantNo: params.merchantNo,
    });
  },

  detail: (errorNo: string) => {
    return request.get<ErrorOrder>(`/api/settlement/error-order/${errorNo}`);
  },

  getById: (id: number) => {
    return request.get<ErrorOrder>(`/api/settlement/error-order/id/${id}`);
  },

  listByDetail: (detailId: number) => {
    return request.get<ErrorOrder[]>(`/api/settlement/error-order/list-by-detail/${detailId}`);
  },

  apply: (data: ErrorOrderApplyRequest, applyUserId?: string, applyUserName?: string) => {
    return request.post<ErrorOrder>('/api/settlement/error-order/apply', data, {
      params: { applyUserId, applyUserName },
    });
  },

  audit: (data: ErrorOrderAuditRequest, auditUserId?: string, auditUserName?: string) => {
    return request.post<ErrorOrder>('/api/settlement/error-order/audit', data, {
      params: { auditUserId, auditUserName },
    });
  },

  processSupplement: (id: number, handleUserId?: string, handleUserName?: string) => {
    return request.post<ErrorOrder>(`/api/settlement/error-order/process-supplement/${id}`, null, {
      params: { handleUserId, handleUserName },
    });
  },

  processRefund: (id: number, handleUserId?: string, handleUserName?: string) => {
    return request.post<ErrorOrder>(`/api/settlement/error-order/process-refund/${id}`, null, {
      params: { handleUserId, handleUserName },
    });
  },

  processAdjust: (id: number, handleUserId?: string, handleUserName?: string) => {
    return request.post<ErrorOrder>(`/api/settlement/error-order/process-adjust/${id}`, null, {
      params: { handleUserId, handleUserName },
    });
  },

  processIgnore: (id: number, handleUserId?: string, handleUserName?: string) => {
    return request.post<ErrorOrder>(`/api/settlement/error-order/process-ignore/${id}`, null, {
      params: { handleUserId, handleUserName },
    });
  },
};
