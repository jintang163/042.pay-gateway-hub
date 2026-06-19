import type { PageParams, PageResult } from './common';

export interface ReportSubscription {
  id: number;
  subscriptionNo: string;
  merchantNo: string;
  reportType: number;
  reportTypeDesc?: string;
  reportCategory: string;
  reportCategoryDesc?: string;
  pushChannel: number;
  pushChannelDesc?: string;
  emailList: string;
  phoneList?: string;
  pushTime: string;
  enabled: number;
  enabledDesc?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ReportSubscriptionSaveRequest {
  merchantNo?: string;
  reportType: number;
  reportCategory?: string;
  pushChannel?: number;
  emailList: string;
  phoneList?: string;
  pushTime?: string;
  enabled?: number;
  remark?: string;
}

export interface ReportPushRecord {
  id: number;
  recordNo: string;
  subscriptionNo?: string;
  merchantNo: string;
  reportType: number;
  reportTypeDesc?: string;
  reportCategory: string;
  reportTitle: string;
  reportPeriod: string;
  startDate: string;
  endDate: string;
  pushStatus: number;
  pushStatusDesc?: string;
  pushChannel: number;
  emailTargets?: string;
  phoneTargets?: string;
  fileUrl?: string;
  fileSize?: number;
  successCount: number;
  failCount: number;
  failReason?: string;
  triggerType: number;
  triggerTypeDesc?: string;
  pushTime?: string;
  createdAt?: string;
}

export interface ReportSubscriptionQueryParams extends PageParams {
  subscriptionNo?: string;
  merchantNo?: string;
  reportType?: number;
  reportCategory?: string;
  pushChannel?: number;
  enabled?: number;
  current?: number;
  size?: number;
}

export interface ReportPushRecordQueryParams extends PageParams {
  recordNo?: string;
  subscriptionNo?: string;
  merchantNo?: string;
  reportType?: number;
  reportCategory?: string;
  pushStatus?: number;
  triggerType?: number;
  current?: number;
  size?: number;
}

export type ReportSubscriptionListResult = PageResult<ReportSubscription>;
export type ReportPushRecordListResult = PageResult<ReportPushRecord>;

export const reportTypeMap: Record<number, string> = {
  1: '日报',
  2: '周报',
};

export const pushChannelMap: Record<number, string> = {
  1: '邮件',
  2: '邮件+短信',
};

export const enabledMap: Record<number, { text: string; color: string }> = {
  0: { text: '禁用', color: 'default' },
  1: { text: '启用', color: 'success' },
};

export const pushStatusMap: Record<number, { text: string; color: string }> = {
  0: { text: '待推送', color: 'gold' },
  1: { text: '推送中', color: 'processing' },
  2: { text: '成功', color: 'success' },
  3: { text: '失败', color: 'error' },
};

export const triggerTypeMap: Record<number, string> = {
  1: '定时任务',
  2: '手动触发',
};

export const reportCategoryOptions = [
  { label: '交易报表', value: 'TRADE' },
  { label: '结算报表', value: 'SETTLEMENT' },
  { label: '退款报表', value: 'REFUND' },
  { label: '手续费报表', value: 'FEE' },
  { label: '对账报表', value: 'RECONCILE' },
];

export const getReportCategoryDesc = (value: string): string => {
  const item = reportCategoryOptions.find(opt => opt.value === value);
  return item ? item.label : value;
};
