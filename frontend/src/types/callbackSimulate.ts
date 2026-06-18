export type CallbackType = 'PAY' | 'REFUND';

export type SimulateStatus = 'SUCCESS' | 'FAIL';

export type SignType = 'MD5' | 'RSA' | 'SM2';

export type CallbackStatus = 0 | 1 | 2 | 3;

export type SignCodeLanguage = 'JAVA' | 'PHP' | 'PYTHON';

export interface CallbackSimulateLog {
  id: number;
  logNo: string;
  merchantNo: string;
  merchantName?: string;
  orderNo?: string;
  callbackUrl: string;
  callbackType: CallbackType;
  simulateStatus: SimulateStatus;
  signType: SignType;
  requestHeaders?: string;
  requestBody?: string;
  responseHttpStatus?: number;
  responseBody?: string;
  responseTimeMs?: number;
  callbackStatus: CallbackStatus;
  callbackStatusDesc?: string;
  retryCount: number;
  operatorName?: string;
  remark?: string;
  createdAt: string;
}

export interface CallbackSimulateRequest {
  merchantNo: string;
  callbackUrl?: string;
  callbackType: CallbackType;
  simulateStatus: SimulateStatus;
  signType?: SignType;
  orderNo?: string;
  amount?: number;
  customRequestBody?: string;
  remark?: string;
}

export interface CallbackSimulateQueryParams {
  current?: number;
  size?: number;
  merchantNo?: string;
  callbackType?: CallbackType;
  simulateStatus?: SimulateStatus;
  callbackStatus?: CallbackStatus;
}

export interface CallbackSimulatePageResult {
  records: CallbackSimulateLog[];
  total: number;
  current: number;
  size: number;
  pages: number;
}

export interface CallbackResendRequest {
  logNo: string;
}

export interface SignCodeExampleRequest {
  signType: SignType;
  language: SignCodeLanguage;
  params?: Record<string, unknown>;
  key?: string;
}

export interface SignCodeExample {
  language: string;
  signType: string;
  code: string;
  description: string;
}
