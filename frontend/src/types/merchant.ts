import type { PageParams, PageResult } from './common';

export type MerchantStatus = 'pending' | 'approved' | 'rejected' | 'suspended' | 'terminated';

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
