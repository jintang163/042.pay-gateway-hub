import type { PageParams, PageResult } from './common';

export type SettlementStatus = 'pending' | 'settling' | 'success' | 'failed';

export interface Settlement {
  id: string;
  settlementNo: string;
  merchantId: string;
  merchantName?: string;
  periodStart: string;
  periodEnd: string;
  totalAmount: number;
  totalFee: number;
  settlementAmount: number;
  currency: string;
  orderCount: number;
  refundCount: number;
  refundAmount: number;
  bankAccount?: string;
  bankName?: string;
  accountHolder?: string;
  status: SettlementStatus;
  settleTime?: string;
  failReason?: string;
  createTime: string;
  updateTime?: string;
}

export interface SettlementQueryParams extends PageParams {
  settlementNo?: string;
  merchantId?: string;
  status?: SettlementStatus;
  startTime?: string;
  endTime?: string;
}

export type SettlementListResult = PageResult<Settlement>;

export type SplitRuleStatus = 'enabled' | 'disabled';

export interface SplitRule {
  id: string;
  ruleId: string;
  ruleName: string;
  merchantId: string;
  merchantName?: string;
  description?: string;
  ruleContent: string;
  status: SplitRuleStatus;
  createTime: string;
  updateTime?: string;
}

export interface SplitRuleQueryParams extends PageParams {
  ruleId?: string;
  ruleName?: string;
  merchantId?: string;
  status?: SplitRuleStatus;
}

export type SplitRuleListResult = PageResult<SplitRule>;

export interface SplitRuleCreateRequest {
  ruleName: string;
  merchantId: string;
  description?: string;
  ruleContent: string;
  status?: SplitRuleStatus;
}
