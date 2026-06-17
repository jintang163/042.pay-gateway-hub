import type { PageParams, PageResult } from './common';

export type SettleStatus = 0 | 1 | 2 | 3;

export interface Settlement {
  id: number;
  settlementNo: string;
  merchantNo: string;
  settleDate: string;
  totalAmount: number;
  feeAmount: number;
  actualSettleAmount: number;
  orderCount: number;
  payChannel: string;
  settleStatus: SettleStatus;
  bankName: string;
  bankAccount: string;
  accountName: string;
  failReason?: string;
  retryCount?: number;
  settleTime?: string;
  createTime: string;
}

export interface SettlementQueryParams extends PageParams {
  settlementNo?: string;
  settleStatus?: SettleStatus;
  settleDateStart?: string;
  settleDateEnd?: string;
  payChannel?: string;
}

export type SettlementListResult = PageResult<Settlement>;

export interface SplitDetail {
  id: number;
  splitDetailNo: string;
  orderNo: string;
  merchantNo: string;
  receiverAccount: string;
  receiverName: string;
  splitType: 'PERCENT' | 'FIXED';
  splitValue: number;
  splitAmount: number;
  status: number;
  remark?: string;
  settleTime?: string;
  createTime: string;
  statusDesc: string;
}

export interface SplitDetailQueryParams extends PageParams {
  splitDetailNo?: string;
  orderNo?: string;
  receiverAccount?: string;
  receiverName?: string;
  status?: number;
  startTime?: string;
  endTime?: string;
}

export type SplitDetailListResult = PageResult<SplitDetail>;

export interface SplitRule {
  id: number;
  ruleNo: string;
  ruleName: string;
  merchantNo: string;
  splitDetails: string;
  status: number;
  statusDesc: string;
  createdAt: string;
  updatedAt: string;
}

export interface SplitRuleQueryParams extends PageParams {
  ruleNo?: string;
  ruleName?: string;
  merchantNo?: string;
  status?: number;
}

export type SplitRuleListResult = PageResult<SplitRule>;

export interface SplitRuleItem {
  receiverAccount: string;
  receiverName: string;
  splitValue: number;
}

export interface SplitRuleSaveRequest {
  id?: number;
  ruleName: string;
  merchantNo: string;
  splitType: 'PERCENT' | 'FIXED';
  splitDetails: string;
  status: number;
}

export interface SplitCalculateRequest {
  orderAmount: number;
  splitType: 'PERCENT' | 'FIXED';
  details: SplitRuleItem[];
}
