import type { PageResult } from './common';

export interface PaymentPageTemplate {
  code: string;
  name: string;
  description: string;
  preview: string;
}

export interface PaymentPageConfig {
  id?: number;
  merchantNo: string;
  merchantName?: string;
  pageTitle?: string;
  logoUrl?: string;
  primaryColor?: string;
  secondaryColor?: string;
  backgroundColor?: string;
  textColor?: string;
  buttonColor?: string;
  buttonTextColor?: string;
  colorSchemeCode?: string;
  customCss?: string;
  footerText?: string;
  returnUrl?: string;
  status?: number;
  statusDesc?: string;
  createdAt?: string;
  updatedAt?: string;
  pageUrl?: string;
}

export interface PaymentPageConfigSaveRequest {
  merchantNo: string;
  pageTitle?: string;
  logoUrl?: string;
  primaryColor?: string;
  secondaryColor?: string;
  backgroundColor?: string;
  textColor?: string;
  buttonColor?: string;
  buttonTextColor?: string;
  colorSchemeCode?: string;
  customCss?: string;
  footerText?: string;
  returnUrl?: string;
  status?: number;
}

export interface PaymentPageQueryParams {
  current?: number;
  size?: number;
  merchantNo?: string;
  merchantName?: string;
  templateCode?: string;
  status?: number;
}

export type PaymentPageConfigListResult = PageResult<PaymentPageConfig>;

export interface PaymentOrderPreview {
  orderNo: string;
  merchantOrderNo: string;
  amount: number;
  productName: string;
  productDescription?: string;
  payChannels: Array<{
    code: string;
    name: string;
    icon: string;
  }>;
}
