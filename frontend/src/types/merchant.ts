import type { PageParams, PageResult } from './common';

export type MerchantStatus = 'pending' | 'approved' | 'rejected' | 'suspended' | 'terminated';

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export type AuditStepStatus = 'done' | 'active' | 'pending';

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
  steps: AuditStepItem[];
}

export interface Merchant {
  id: string;
  merchantNo: string;
  merchantName: string;
  shortName?: string;
  businessLicense?: string;
  legalPerson?: string;
  legalPersonIdCard?: string;
  contactName: string;
  contactPhone: string;
  contactEmail: string;
  address?: string;
  settleBankName?: string;
  settleBankAccount?: string;
  settleAccountName?: string;
  status: MerchantStatus;
  applyTime?: string;
  approveTime?: string;
  approver?: string;
  rejectReason?: string;
  createTime: string;
  updateTime?: string;
  auditStep?: number;
  auditStepName?: string;
  riskLevel?: RiskLevel;
  riskLevelDesc?: string;
  riskScore?: number;
  businessVerifyPassed?: number;
  businessVerifyTime?: string;
  autoAuditPassed?: number;
  autoAuditRemark?: string;
  autoAuditTime?: string;
}

export interface MerchantApplyRequest {
  merchantName: string;
  shortName?: string;
  businessLicense: string;
  legalPerson: string;
  legalPersonIdCard: string;
  contactPhone: string;
  contactEmail: string;
  settleBankName: string;
  settleBankAccount: string;
  settleAccountName: string;
  address?: string;
}

export interface MerchantQueryParams extends PageParams {
  merchantNo?: string;
  merchantName?: string;
  status?: MerchantStatus;
  contactPhone?: string;
}

export type MerchantListResult = PageResult<Merchant>;
