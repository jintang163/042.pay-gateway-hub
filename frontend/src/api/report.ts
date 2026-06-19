import { request } from '@/utils/request';
import type {
  ReportSubscription,
  ReportSubscriptionSaveRequest,
  ReportSubscriptionQueryParams,
  ReportSubscriptionListResult,
  ReportPushRecord,
  ReportPushRecordQueryParams,
  ReportPushRecordListResult,
} from '@/types/report';

export const reportApi = {
  subscriptionPage: (params: ReportSubscriptionQueryParams) => {
    return request.get<ReportSubscriptionListResult>('/api/report/subscription/page', params);
  },

  getSubscription: (id: number) => {
    return request.get<ReportSubscription>(`/api/report/subscription/${id}`);
  },

  saveSubscription: (data: ReportSubscriptionSaveRequest) => {
    return request.post<ReportSubscription>('/api/report/subscription', data);
  },

  updateSubscription: (id: number, data: ReportSubscriptionSaveRequest) => {
    return request.put<ReportSubscription>(`/api/report/subscription/${id}`, data);
  },

  deleteSubscription: (id: number) => {
    return request.delete<void>(`/api/report/subscription/${id}`);
  },

  toggleSubscription: (id: number) => {
    return request.post<ReportSubscription>(`/api/report/subscription/${id}/toggle`);
  },

  manualPush: (id: number) => {
    return request.post<void>(`/api/report/subscription/${id}/push`);
  },

  pushRecordPage: (params: ReportPushRecordQueryParams) => {
    return request.get<ReportPushRecordListResult>('/api/report/push-record/page', params);
  },

  getPushRecord: (id: number) => {
    return request.get<ReportPushRecord>(`/api/report/push-record/${id}`);
  },
};
