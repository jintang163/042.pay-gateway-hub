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

export type ReconcileStatus = 0 | 1 | 2;
export type DiffType = 1 | 2 | 3 | 4;
export type HandleStatus = 0 | 1 | 2 | 3;
export type ErrorStatus = 0 | 1 | 2 | 3 | 4;
export type ErrorHandleType = 1 | 2 | 3 | 4;
export type ErrorAuditStatus = 0 | 1 | 2;

export interface Reconcile {
  id: number;
  reconcileNo: string;
  reconcileDate: string;
  payChannel: string;
  totalCount: number;
  matchCount: number;
  mismatchCount: number;
  reconcileStatus: ReconcileStatus;
  reconcileStatusDesc: string;
  createdAt: string;
  updatedAt: string;
}

export interface ReconcileQueryParams extends PageParams {
  payChannel?: string;
  reconcileDate?: string;
  reconcileStatus?: ReconcileStatus;
}

export type ReconcileListResult = PageResult<Reconcile>;

export interface ReconcileDetail {
  id: number;
  detailNo: string;
  reconcileNo: string;
  reconcileDate: string;
  payChannel: string;
  payChannelDesc: string;
  diffType: DiffType;
  diffTypeDesc: string;
  orderNo?: string;
  merchantNo?: string;
  channelTradeNo?: string;
  localAmount?: number;
  channelAmount?: number;
  diffAmount?: number;
  localStatus?: number;
  localStatusDesc?: string;
  channelStatus?: string;
  localPayTime?: string;
  channelPayTime?: string;
  errorOrderNo?: string;
  handleStatus: HandleStatus;
  handleStatusDesc: string;
  handleRemark?: string;
  handleUserId?: string;
  handleUserName?: string;
  handleTime?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ReconcileDetailQueryParams extends PageParams {
  reconcileNo?: string;
  reconcileDate?: string;
  payChannel?: string;
  diffType?: DiffType;
  handleStatus?: HandleStatus;
  orderNo?: string;
  merchantNo?: string;
  channelTradeNo?: string;
}

export type ReconcileDetailListResult = PageResult<ReconcileDetail>;

export interface ReconcileSummary {
  reconcileNo?: string;
  reconcileDate?: string;
  payChannel?: string;
  totalCount: number;
  matchCount: number;
  mismatchCount: number;
  longFund: {
    count: number;
    totalAmount: number;
  };
  shortFund: {
    count: number;
    totalAmount: number;
  };
  amountMismatch: {
    count: number;
    totalDiffAmount: number;
  };
  statusMismatch: {
    count: number;
  };
}

export interface ErrorOrder {
  id: number;
  errorNo: string;
  reconcileNo?: string;
  reconcileDetailId?: number;
  payChannel: string;
  payChannelDesc: string;
  errorType: DiffType;
  errorTypeDesc: string;
  handleType?: ErrorHandleType;
  handleTypeDesc?: string;
  orderNo?: string;
  merchantNo?: string;
  channelTradeNo?: string;
  orderAmount?: number;
  actualAmount?: number;
  diffAmount?: number;
  errorStatus: ErrorStatus;
  errorStatusDesc: string;
  applyUserId?: string;
  applyUserName?: string;
  applyTime?: string;
  applyRemark?: string;
  auditUserId?: string;
  auditUserName?: string;
  auditTime?: string;
  auditRemark?: string;
  auditStatus?: ErrorAuditStatus;
  auditStatusDesc?: string;
  handleUserId?: string;
  handleUserName?: string;
  handleTime?: string;
  handleResult?: string;
  refundNo?: string;
  newOrderNo?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ErrorOrderQueryParams extends PageParams {
  errorNo?: string;
  reconcileNo?: string;
  payChannel?: string;
  errorType?: DiffType;
  errorStatus?: ErrorStatus;
  auditStatus?: ErrorAuditStatus;
  handleType?: ErrorHandleType;
  orderNo?: string;
  merchantNo?: string;
}

export type ErrorOrderListResult = PageResult<ErrorOrder>;

export interface ErrorOrderApplyRequest {
  reconcileDetailId: number;
  handleType: ErrorHandleType;
  applyRemark?: string;
  adjustAmount?: number;
}

export interface ErrorOrderAuditRequest {
  errorOrderId: number;
  auditStatus: ErrorAuditStatus;
  auditRemark?: string;
}
