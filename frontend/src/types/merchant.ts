import type { PageResult } from './common';

export type MerchantStatus = 'pending' | 'approved' | 'rejected' | 'suspended' | 'terminated';

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export type AuditStepStatus = 'done' | 'active' | 'pending';

export interface MerchantApplyResult {
  merchantNo: string;
  merchantName: string;
  auditStep: number;
  auditStepName: string;
  auditStatus: number;
  auditStatusDesc: string;
}

export interface VerifyDetail {
  verifyId?: string;
  verifyVendor?: string;
  verifySource?: string;
  verifyRequestId?: string;
  fallbackUsed?: boolean;
  matchOverallScore?: number;
  decisionReasons?: string[];
  failReason?: string;
  rawRequest?: string;
  rawResponse?: string;
  verifiedBy?: string;
  verifyTime?: string;
}

export interface AuditStepItem {
  step: number;
  name: string;
  description: string;
  status: AuditStepStatus;
  time?: string;
  remark?: string;
}

export interface AuditProgress {
  merchantNo: string;
  merchantName: string;
  auditStatus: number;
  auditStatusDesc: string;
  auditStep: number;
  auditStepName: string;
  auditStepDescription: string;
  riskLevel?: RiskLevel;
  riskLevelDesc?: string;
  riskScore?: number;
  businessVerifyPassed?: number;
  businessVerifyResult?: string;
  businessVerifyTime?: string;
  autoAuditPassed?: number;
  autoAuditRemark?: string;
  autoAuditTime?: string;
  manualAuditUser?: string;
  manualAuditTime?: string;
  auditRemark?: string;
  steps?: AuditStepItem[];
  verifyDetail?: VerifyDetail;
}

export interface Merchant {
  id?: number;
  merchantNo: string;
  merchantName: string;
  shortName?: string;
  businessLicenseNo: string;
  legalPersonName: string;
  legalPersonIdNo?: string;
  contactPhone: string;
  contactEmail: string;
  address?: string;
  settleBankName?: string;
  settleBankAccount?: string;
  settleAccountName?: string;
  auditStatus?: number;
  auditStatusDesc?: string;
  auditRemark?: string;
  status?: number;
  statusDesc?: string;
  createTime?: string;
  updateTime?: string;
  auditStep?: number;
  auditStepName?: string;
  riskLevel?: RiskLevel;
  riskLevelDesc?: string;
  riskScore?: number;
  industryCode?: string;
  industryName?: string;
  businessVerifyPassed?: number;
  businessVerifyResult?: string;
  businessVerifyTime?: string;
  autoAuditPassed?: number;
  autoAuditRemark?: string;
  autoAuditTime?: string;
  manualAuditUser?: string;
  manualAuditTime?: string;
}

export interface MerchantQueryParams {
  current?: number;
  size?: number;
  merchantName?: string;
  merchantNo?: string;
  auditStatus?: number;
  riskLevel?: RiskLevel | '';
  status?: number;
}

export interface MerchantApplyRequest {
  merchantName: string;
  businessLicenseNo: string;
  legalPersonName: string;
  legalPersonIdNo: string;
  contactPhone: string;
  contactEmail: string;
  settlementBankName: string;
  settlementBankAccount: string;
  settlementAccountName: string;
  [key: string]: any;
}

export type MerchantListResult = PageResult<Merchant>;
