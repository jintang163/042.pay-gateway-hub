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

export interface FailReasonVO {
  code: string;
  message: string;
  category: 'BALANCE' | 'RISK' | 'CHANNEL' | 'SIGN' | 'PARAM' | 'MERCHANT' | 'USER' | 'SYSTEM' | 'OTHER';
  suggestion: string;
  ruleDescription: string;
  priority: number;
}

export interface OrderAttributionVO {
  orderNo: string;
  failCode?: string;
  failMessage?: string;
  failCategory?: string;
  suggestion?: string;
  ruleDescription?: string;
  priority?: number;
  evidence?: string[];
  orderInfo?: {
    orderNo: string;
    merchantNo: string;
    merchantOrderNo?: string;
    payAmount: number;
    payChannel: string;
    payType: string;
    payStatus: number;
    expireTime?: string;
    createdAt?: string;
  };
  latestChannelLog?: {
    id?: number;
    orderNo?: string;
    channelCode?: string;
    requestType?: string;
    requestUrl?: string;
    requestData?: string;
    responseData?: string;
    errorMsg?: string;
    costTime?: number;
    createTime?: string;
  };
  latestRiskLog?: {
    id?: number;
    merchantNo?: string;
    orderNo?: string;
    riskType?: string;
    riskLevel?: string;
    riskRule?: string;
    riskDesc?: string;
    handleResult?: number;
    handleDesc?: string;
    triggerTime?: string;
  };
}
