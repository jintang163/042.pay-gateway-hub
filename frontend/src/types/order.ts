import type { PageParams, PageResult } from './common';

export type PayMethod = 'alipay' | 'wechat' | 'unionpay' | 'applepay';
export type PayChannel = 'native' | 'h5' | 'jsapi' | 'app' | 'miniapp';
export type OrderStatus = 'pending' | 'paying' | 'success' | 'failed' | 'closed' | 'refunding' | 'refunded';

export interface Order {
  id: string;
  orderNo: string;
  merchantOrderNo: string;
  merchantId: string;
  merchantName?: string;
  amount: number;
  currency: string;
  payMethod: PayMethod;
  payChannel: PayChannel;
  status: OrderStatus;
  subject?: string;
  buyerId?: string;
  buyerName?: string;
  transactionId?: string;
  paidTime?: string;
  createTime: string;
  updateTime?: string;
  remark?: string;
}

export interface OrderQueryParams extends PageParams {
  orderNo?: string;
  merchantOrderNo?: string;
  merchantId?: string;
  payMethod?: PayMethod;
  payChannel?: PayChannel;
  status?: OrderStatus;
  startTime?: string;
  endTime?: string;
  buyerId?: string;
}

export type OrderListResult = PageResult<Order>;

export interface OrderDetail extends Order {
  payUrl?: string;
  qrCode?: string;
  failReason?: string;
  fee?: number;
  settlementAmount?: number;
}
