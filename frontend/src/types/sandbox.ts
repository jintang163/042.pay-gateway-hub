import type { PageParams, PageResult } from './common';

export type SandboxStatus = 'success' | 'failed' | 'pending';

export type SandboxScene =
  | 'success'
  | 'failed'
  | 'timeout'
  | 'repeat_notify'
  | 'sign_error'
  | 'amount_mismatch';

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
  merchantId: string;
  payMethod: string;
  payChannel: string;
  testType: SandboxTest['testType'];
  scene: SandboxScene;
  testAmount?: number;
  orderNo?: string;
  extraParams?: Record<string, unknown>;
}
