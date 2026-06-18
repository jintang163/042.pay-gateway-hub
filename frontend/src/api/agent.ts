import { request } from '@/utils/request';
import type {
  AgentRelation,
  AgentRelationSaveRequest,
  AgentRelationQueryParams,
  AgentRelationListResult,
  AgentTree,
  AgentStats,
  AgentProfitRule,
  AgentProfitRuleSaveRequest,
  AgentProfitRuleQueryParams,
  AgentProfitRuleListResult,
  AgentProfitRecord,
  AgentProfitRecordQueryParams,
  AgentProfitRecordListResult,
  AgentWithdraw,
  AgentWithdrawApplyRequest,
  AgentWithdrawAuditRequest,
  AgentWithdrawQueryParams,
  AgentWithdrawListResult,
} from '@/types/agent';

export const agentRelationApi = {
  list: (params: AgentRelationQueryParams) => {
    return request.get<AgentRelationListResult>('/api/agent/relation/list', {
      current: params.pageNum,
      size: params.pageSize,
      merchantNo: params.merchantNo,
      merchantName: params.merchantName,
      parentMerchantNo: params.parentMerchantNo,
      agentLevel: params.agentLevel,
      status: params.status,
    });
  },

  detail: (id: number) => {
    return request.get<AgentRelation>(`/api/agent/relation/${id}`);
  },

  getByMerchantNo: (merchantNo: string) => {
    return request.get<AgentRelation>(`/api/agent/relation/by-merchant/${merchantNo}`);
  },

  save: (data: AgentRelationSaveRequest) => {
    return request.post<{ id: number }>('/api/agent/relation/save', data);
  },

  delete: (id: number) => {
    return request.post<void>(`/api/agent/relation/delete/${id}`);
  },

  updateStatus: (id: number, status: number) => {
    return request.post<void>(`/api/agent/relation/${id}/status`, null, { params: { status } });
  },

  getTree: (merchantNo: string) => {
    return request.get<AgentTree[]>(`/api/agent/relation/tree/${merchantNo}`);
  },

  listDirectSubordinates: (parentMerchantNo: string) => {
    return request.get<AgentRelation[]>(`/api/agent/relation/subordinates/direct/${parentMerchantNo}`);
  },

  listAllSubordinates: (merchantNo: string) => {
    return request.get<AgentRelation[]>(`/api/agent/relation/subordinates/all/${merchantNo}`);
  },

  getStats: (merchantNo: string) => {
    return request.get<AgentStats>(`/api/agent/relation/stats/${merchantNo}`);
  },
};

export const agentProfitRuleApi = {
  list: (params: AgentProfitRuleQueryParams) => {
    return request.get<AgentProfitRuleListResult>('/api/agent/profit-rule/list', {
      current: params.pageNum,
      size: params.pageSize,
      ruleNo: params.ruleNo,
      ruleName: params.ruleName,
      merchantNo: params.merchantNo,
      agentLevel: params.agentLevel,
      status: params.status,
    });
  },

  detail: (id: number) => {
    return request.get<AgentProfitRule>(`/api/agent/profit-rule/${id}`);
  },

  getByRuleNo: (ruleNo: string) => {
    return request.get<AgentProfitRule>(`/api/agent/profit-rule/by-rule-no/${ruleNo}`);
  },

  listByMerchant: (merchantNo: string) => {
    return request.get<AgentProfitRule[]>(`/api/agent/profit-rule/listByMerchant/${merchantNo}`);
  },

  save: (data: AgentProfitRuleSaveRequest) => {
    return request.post<{ id: number; ruleNo: string }>('/api/agent/profit-rule/save', data);
  },

  delete: (id: number) => {
    return request.post<void>(`/api/agent/profit-rule/delete/${id}`);
  },

  toggle: (id: number) => {
    return request.post<void>(`/api/agent/profit-rule/${id}/toggle`);
  },
};

export const agentProfitApi = {
  list: (params: AgentProfitRecordQueryParams) => {
    return request.get<AgentProfitRecordListResult>('/api/agent/profit/list', {
      current: params.pageNum,
      size: params.pageSize,
      profitNo: params.profitNo,
      orderNo: params.orderNo,
      merchantNo: params.merchantNo,
      agentMerchantNo: params.agentMerchantNo,
      agentLevel: params.agentLevel,
      profitStatus: params.profitStatus,
      settleDate: params.settleDate,
      startDate: params.startDate,
      endDate: params.endDate,
    });
  },

  detail: (id: number) => {
    return request.get<AgentProfitRecord>(`/api/agent/profit/${id}`);
  },

  listByAgent: (agentMerchantNo: string) => {
    return request.get<AgentProfitRecord[]>(`/api/agent/profit/list-by-agent/${agentMerchantNo}`);
  },

  getTotal: (agentMerchantNo: string, profitStatus?: number) => {
    return request.get<number>(`/api/agent/profit/total/${agentMerchantNo}`, { profitStatus });
  },

  settle: (settleDate: string) => {
    return request.post<void>(`/api/agent/profit/settle/${settleDate}`);
  },
};

export const agentWithdrawApi = {
  list: (params: AgentWithdrawQueryParams) => {
    return request.get<AgentWithdrawListResult>('/api/agent/withdraw/list', {
      current: params.pageNum,
      size: params.pageSize,
      withdrawNo: params.withdrawNo,
      merchantNo: params.merchantNo,
      merchantName: params.merchantName,
      withdrawStatus: params.withdrawStatus,
      startDate: params.startDate,
      endDate: params.endDate,
    });
  },

  detail: (id: number) => {
    return request.get<AgentWithdraw>(`/api/agent/withdraw/${id}`);
  },

  getByWithdrawNo: (withdrawNo: string) => {
    return request.get<AgentWithdraw>(`/api/agent/withdraw/by-withdraw-no/${withdrawNo}`);
  },

  listByMerchant: (merchantNo: string) => {
    return request.get<AgentWithdraw[]>(`/api/agent/withdraw/list-by-merchant/${merchantNo}`);
  },

  getTotal: (merchantNo: string, withdrawStatus?: number) => {
    return request.get<number>(`/api/agent/withdraw/total/${merchantNo}`, { withdrawStatus });
  },

  getBalance: (merchantNo: string) => {
    return request.get<number>(`/api/agent/withdraw/balance/${merchantNo}`);
  },

  apply: (data: AgentWithdrawApplyRequest) => {
    return request.post<{ id: number; withdrawNo: string }>('/api/agent/withdraw/apply', data);
  },

  audit: (data: AgentWithdrawAuditRequest) => {
    return request.post<void>('/api/agent/withdraw/audit', data);
  },

  execute: (id: number) => {
    return request.post<void>(`/api/agent/withdraw/execute/${id}`);
  },

  retry: () => {
    return request.post<void>('/api/agent/withdraw/retry');
  },
};
