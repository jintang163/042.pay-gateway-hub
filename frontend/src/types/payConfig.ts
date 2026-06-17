import type { PageParams, PageResult } from './common';

export type PayMethod = 'alipay' | 'wechat' | 'unionpay' | 'applepay';
export type PayChannel = 'native' | 'h5' | 'jsapi' | 'app' | 'miniapp';
export type ConfigStatus = 'enabled' | 'disabled';

export interface PayConfig {
  id: string;
  configId: string;
  merchantId: string;
  merchantName?: string;
  payMethod: PayMethod;
  payChannel: PayChannel;
  channelMerchantId?: string;
  channelAppId?: string;
  channelSecret?: string;
  publicKey?: string;
  privateKey?: string;
  certPath?: string;
  notifyUrl?: string;
  callbackUrl?: string;
  whitelistIps?: string;
  splitRule?: string;
  feeRate: number;
  singleLimit?: number;
  dailyLimit?: number;
  status: ConfigStatus;
  createTime: string;
  updateTime?: string;
  remark?: string;
}

export interface PayConfigQueryParams extends PageParams {
  merchantId?: string;
  payMethod?: PayMethod;
  payChannel?: PayChannel;
  status?: ConfigStatus;
}

export type PayConfigListResult = PageResult<PayConfig>;

export interface PayConfigCreateRequest {
  merchantId: string;
  payMethod: PayMethod;
  payChannel: PayChannel;
  channelMerchantId?: string;
  channelAppId?: string;
  channelSecret?: string;
  publicKey?: string;
  privateKey?: string;
  certPath?: string;
  notifyUrl?: string;
  callbackUrl?: string;
  whitelistIps?: string;
  splitRule?: string;
  feeRate: number;
  singleLimit?: number;
  dailyLimit?: number;
  remark?: string;
}
