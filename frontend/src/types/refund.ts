import type { PageParams, PageResult } from './common';

export type RefundStatus = 'pending' | 'processing' | 'success' | 'failed' | 'canceled';

export interface Refund {
  id: string;
  refundNo: string;
  orderId: string;
  orderNo: string;
  merchantId: string;
  merchantName?: string;
  refundAmount: number;
  orderAmount: number;
  currency: string;
  reason?: string;
  status: RefundStatus;
  operator?: string;
  refundTime?: string;
  failReason?: string;
  createTime: string;
  updateTime?: string;
}

export interface RefundQueryParams extends PageParams {
  refundNo?: string;
  orderNo?: string;
  merchantId?: string;
  status?: RefundStatus;
  startTime?: string;
  endTime?: string;
}

export type RefundListResult = PageResult<Refund>;

export interface RefundApplyRequest {
  orderId: string;
  refundAmount: number;
  reason: string;
  notifyUrl?: string;
}
