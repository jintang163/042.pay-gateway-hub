import type { PageParams, PageResult } from './common';

export type SandboxStatus = 'success' | 'failed' | 'pending';

export type SandboxScene =
  | 'success'
  | 'failed'
  | 'timeout'
  | 'insufficient_balance'
  | 'repeat_notify'
  | 'sign_error'
  | 'amount_mismatch'
  | 'refund_success'
  | 'refund_failed'
  | 'channel_error';

export interface SandboxTestResult {
  success: boolean;
  testId: string;
  scene: SandboxScene;
  orderNo: string;
  requestTime: string;
  responseTime: string;
  duration: number;
  requestParams: string;
  responseData: string;
  notifyResult?: {
    notified: boolean;
    notifyCount: number;
    notifyUrl: string;
    lastNotifyTime: string;
  };
  errorMessage?: string;
}

export interface SandboxSceneOption {
  code: string;
  name: string;
  description?: string;
}

export interface SandboxMerchant {
  id: string;
  merchantName: string;
  description?: string;
}

export interface SandboxPayMethod {
  code: string;
  name: string;
  channels: { code: string; name: string }[];
}

export interface SandboxTest {
  id: string;
  testId: string;
  merchantId: string;
  merchantName?: string;
  payMethod: string;
  payChannel: string;
  testAmount: number;
  testType: 'pay' | 'refund' | 'query';
  scene: SandboxScene;
  status: SandboxStatus;
  requestParams?: Record<string, unknown>;
  responseResult?: Record<string, unknown>;
  errorMessage?: string;
  spendTime?: number;
  createTime: string;
  operator?: string;
}

export interface SandboxQueryParams extends PageParams {
  testId?: string;
  merchantId?: string;
  testType?: SandboxTest['testType'];
  scene?: SandboxScene;
  status?: SandboxStatus;
  startTime?: string;
  endTime?: string;
}

export type SandboxListResult = PageResult<SandboxTest>;

export interface SandboxExecuteRequest {
  merchantNo: string;
  testScene: string;
  testName: string;
  payChannel: string;
  payType: string;
  payAmount: number;
  notifyUrl?: string;
  extraParams?: string;
}
