import type { PageParams, PageResult } from './common';

export type AgentProfitStatus = 0 | 1 | 2;
export type AgentWithdrawStatus = 0 | 1 | 2 | 3 | 4 | 5;
export type AgentSettleType = 0 | 1;

export interface AgentRelation {
  id: number;
  merchantNo: string;
  merchantName: string;
  parentMerchantNo: string;
  parentMerchantName: string;
  agentLevel: number;
  agentPath: string;
  commissionRate: number;
  status: number;
  statusDesc: string;
  remark?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AgentRelationSaveRequest {
  id?: number;
  merchantNo: string;
  merchantName?: string;
  parentMerchantNo?: string;
  parentMerchantName?: string;
  agentLevel?: number;
  commissionRate?: number;
  status?: number;
  remark?: string;
}

export interface AgentRelationQueryParams extends PageParams {
  merchantNo?: string;
  merchantName?: string;
  parentMerchantNo?: string;
  agentLevel?: number;
  status?: number;
}

export type AgentRelationListResult = PageResult<AgentRelation>;

export interface AgentTree {
  id: number;
  merchantNo: string;
  merchantName: string;
  agentLevel: number;
  commissionRate: number;
  status: number;
  children?: AgentTree[];
}

export interface AgentStats {
  totalAgentCount: number;
  activeAgentCount: number;
  totalProfitAmount: number;
  availableBalance: number;
  frozenAmount: number;
  todayNewAgentCount: number;
  totalSubordinateCount: number;
}

export interface AgentProfitRule {
  id: number;
  ruleNo: string;
  ruleName: string;
  merchantNo: string;
  merchantName: string;
  agentLevel: number;
  commissionRate: number;
  minCommission: number;
  settleType: AgentSettleType;
  settleTypeDesc: string;
  status: number;
  statusDesc: string;
  remark?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AgentProfitRuleSaveRequest {
  id?: number;
  ruleName: string;
  merchantNo: string;
  merchantName?: string;
  agentLevel: number;
  commissionRate: number;
  minCommission?: number;
  settleType?: AgentSettleType;
  status?: number;
  remark?: string;
}

export interface AgentProfitRuleQueryParams extends PageParams {
  ruleNo?: string;
  ruleName?: string;
  merchantNo?: string;
  agentLevel?: number;
  status?: number;
}

export type AgentProfitRuleListResult = PageResult<AgentProfitRule>;

export interface AgentProfitRecord {
  id: number;
  profitNo: string;
  orderNo: string;
  merchantNo: string;
  merchantName: string;
  agentMerchantNo: string;
  agentMerchantName: string;
  agentLevel: number;
  orderAmount: number;
  feeAmount: number;
  profitAmount: number;
  commissionRate: number;
  settleDate: string;
  profitStatus: AgentProfitStatus;
  profitStatusDesc: string;
  settlementId?: string;
  settlementNo?: string;
  remark?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AgentProfitRecordQueryParams extends PageParams {
  profitNo?: string;
  orderNo?: string;
  merchantNo?: string;
  agentMerchantNo?: string;
  agentLevel?: number;
  profitStatus?: AgentProfitStatus;
  settleDate?: string;
  startDate?: string;
  endDate?: string;
}

export type AgentProfitRecordListResult = PageResult<AgentProfitRecord>;

export interface AgentWithdraw {
  id: number;
  withdrawNo: string;
  merchantNo: string;
  merchantName: string;
  withdrawAmount: number;
  actualAmount: number;
  feeAmount: number;
  withdrawStatus: AgentWithdrawStatus;
  withdrawStatusDesc: string;
  bankName: string;
  bankAccount: string;
  accountName: string;
  auditUser?: string;
  auditTime?: string;
  auditRemark?: string;
  transferNo?: string;
  transferChannel?: string;
  transferTime?: string;
  transferFailReason?: string;
  remark?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AgentWithdrawApplyRequest {
  merchantNo: string;
  withdrawAmount: number;
  bankName: string;
  bankAccount: string;
  accountName: string;
  remark?: string;
}

export interface AgentWithdrawAuditRequest {
  id: number;
  auditStatus: number;
  auditRemark?: string;
  auditUser?: string;
}

export interface AgentWithdrawQueryParams extends PageParams {
  withdrawNo?: string;
  merchantNo?: string;
  merchantName?: string;
  withdrawStatus?: AgentWithdrawStatus;
  startDate?: string;
  endDate?: string;
}

export type AgentWithdrawListResult = PageResult<AgentWithdraw>;
