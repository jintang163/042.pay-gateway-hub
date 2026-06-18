import { request } from '@/utils/request';
import type { Merchant, MerchantQueryParams, MerchantListResult, MerchantApplyRequest, AuditProgress, MerchantApplyResult } from '@/types/merchant';

export const merchantApi = {
  list: (params: MerchantQueryParams) => {
    return request.get<MerchantListResult>('/merchant/list', params);
  },

  detail: (merchantNo: string) => {
    return request.get<Merchant>(`/merchant/${merchantNo}`);
  },

  apply: (data: MerchantApplyRequest) => {
    return request.post<MerchantApplyResult>('/merchant/apply', data);
  },

  audit: (data: { merchantNo: string; status: string; remark?: string; auditUserName?: string }) => {
    return request.post<void>('/merchant/audit', {
      merchantNo: data.merchantNo,
      auditStatus: data.status === 'approved' ? 1 : 2,
      auditRemark: data.remark,
      auditUserName: data.auditUserName,
    });
  },

  approve: (id: string) => {
    return request.post<void>(`/merchant/${id}/approve`);
  },

  reject: (id: string, reason: string) => {
    return request.post<void>(`/merchant/${id}/reject`, { reason });
  },

  suspend: (id: string, reason?: string) => {
    return request.post<void>(`/merchant/${id}/suspend`, { reason });
  },

  resume: (id: string) => {
    return request.post<void>(`/merchant/${id}/resume`);
  },

  terminate: (id: string, reason?: string) => {
    return request.post<void>(`/merchant/${id}/terminate`, { reason });
  },

  update: (id: string, data: Partial<Merchant>) => {
    return request.put<void>(`/merchant/${id}`, data);
  },

  resetApiKey: (merchantNo: string) => {
    return request.post<{ apiKey: string }>('/merchant/resetApiKey', { merchantNo });
  },

  testCallback: (data: { merchantNo: string; callbackUrl: string; type?: string }) => {
    return request.post<{ success: boolean; message: string }>('/merchant/testCallback', data);
  },

  getAuditProgress: (merchantNo: string) => {
    return request.get<AuditProgress>(`/merchant/${merchantNo}/audit-progress`);
  },

  getManualAuditStats: () => {
    return request.get<{
      totalPending: number;
      highRisk: number;
      mediumRisk: number;
      lowRisk: number;
      needManual: number;
      businessFail: number;
    }>('/merchant/manual-audit-stats');
  },
};
