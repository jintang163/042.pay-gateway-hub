export interface FeeRule {
  id: number;
  ruleNo: string;
  industryCode: string;
  industryName: string;
  payChannel?: string;
  payChannelDesc?: string;
  minAmount: number;
  maxAmount: number;
  feeRate: number;
  minFee: number;
  maxFee?: number;
  priority: number;
  status: number;
  statusDesc?: string;
  operatorName?: string;
  remark?: string;
  createdAt: string;
}

export interface FeeRuleQueryParams {
  current?: number;
  size?: number;
  industryCode?: string;
  payChannel?: string;
  status?: number;
}

export interface FeeRuleSaveRequest {
  id?: number;
  industryCode: string;
  industryName: string;
  payChannel?: string;
  minAmount: number;
  maxAmount: number;
  feeRate: number;
  minFee?: number;
  maxFee?: number;
  priority?: number;
  status?: number;
  remark?: string;
}

export interface FeeCalcRequest {
  industryCode: string;
  payChannel?: string;
  amount: number;
}

export interface FeeCalcResult {
  amount: number;
  feeAmount: number;
  feeRate: number;
  minFee?: number;
  maxFee?: number;
  ruleNo?: string;
  industryCode: string;
  industryName?: string;
  payChannel?: string;
  calcDetail: string;
}

export interface FeeRulePageResult {
  records: FeeRule[];
  total: number;
  current: number;
  size: number;
  pages: number;
}

export interface IndustryOption {
  code: string;
  name: string;
}
