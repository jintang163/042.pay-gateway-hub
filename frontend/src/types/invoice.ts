import type { PageParams, PageResult } from './common';

export type InvoiceChannel = 'NUONUO' | 'BAIWANG';
export type InvoiceStatus = 0 | 1 | 2 | 3 | 10 | 11 | 12 | 13;
export type InvoiceType = 1 | 2;
export type TitleType = 1 | 2;

export interface InvoiceItem {
  itemName: string;
  itemCode?: string;
  specification?: string;
  unit?: string;
  quantity?: number;
  unitPrice?: number;
  amount?: number;
  taxAmount?: number;
  taxRate?: string;
  taxIncludedFlag?: number;
}

export interface Invoice {
  id: number;
  invoiceNo: string;
  merchantNo: string;
  orderNo: string;
  channelInvoiceNo?: string;
  channelCode: InvoiceChannel;
  invoiceType: InvoiceType;
  invoiceStatus: InvoiceStatus;
  invoiceStatusDesc: string;
  titleType: TitleType;
  titleTypeDesc: string;
  buyerTitle: string;
  buyerTaxNo?: string;
  buyerAddress?: string;
  buyerBankName?: string;
  buyerBankAccount?: string;
  buyerPhone?: string;
  buyerEmail?: string;
  invoiceContent: string;
  invoiceAmount: number;
  taxAmount?: number;
  totalAmount?: number;
  taxRate?: string;
  pdfUrl?: string;
  originalInvoiceNo?: string;
  redReason?: string;
  remark?: string;
  failReason?: string;
  issueTime?: string;
  createdAt: string;
  items?: InvoiceItem[];
}

export interface InvoiceApplyRequest {
  orderNo: string;
  channelCode?: InvoiceChannel;
  titleType: TitleType;
  buyerTitle: string;
  buyerTaxNo?: string;
  buyerAddress?: string;
  buyerBankName?: string;
  buyerBankAccount?: string;
  buyerPhone?: string;
  buyerEmail?: string;
  invoiceContent?: string;
  invoiceAmount?: number;
  remark?: string;
  notifyUrl?: string;
  items?: InvoiceItem[];
}

export interface InvoiceRedFlushRequest {
  originalInvoiceNo: string;
  redReason: string;
  notifyUrl?: string;
}

export interface InvoiceQueryParams extends PageParams {
  invoiceNo?: string;
  orderNo?: string;
  channelInvoiceNo?: string;
  channelCode?: InvoiceChannel;
  invoiceType?: InvoiceType;
  invoiceStatus?: InvoiceStatus;
  buyerTitle?: string;
  startTime?: string;
  endTime?: string;
  current?: number;
  size?: number;
}

export type InvoiceListResult = PageResult<Invoice>;

export const invoiceStatusMap: Record<number, { text: string; color: string; status: 'success' | 'processing' | 'error' | 'default' | 'warning' }> = {
  0: { text: '待开票', color: 'orange', status: 'processing' },
  1: { text: '开票中', color: 'blue', status: 'processing' },
  2: { text: '开票成功', color: 'green', status: 'success' },
  3: { text: '开票失败', color: 'red', status: 'error' },
  10: { text: '待红冲', color: 'orange', status: 'processing' },
  11: { text: '红冲中', color: 'blue', status: 'processing' },
  12: { text: '红冲成功', color: 'green', status: 'success' },
  13: { text: '红冲失败', color: 'red', status: 'error' },
};

export const invoiceTypeMap: Record<number, string> = {
  1: '蓝票',
  2: '红票',
};

export const titleTypeMap: Record<number, string> = {
  1: '个人',
  2: '企业',
};

export const channelMap: Record<string, string> = {
  NUONUO: '诺诺发票',
  BAIWANG: '百望发票',
};
