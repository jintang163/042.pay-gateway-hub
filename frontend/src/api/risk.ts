import { request } from '@/utils/request';
import type {
  RiskEvent,
  RiskQueryParams,
  RiskEventListResult,
  RiskDashboardStats,
  RuleType,
  ActionType,
  RiskRule,
  RiskRuleQueryParams,
  RiskRuleSaveRequest,
  ListType,
  RiskList,
  RiskListQueryParams,
  RiskListSaveRequest,
  RiskDevice,
  RiskDeviceQueryParams,
  RiskAudit,
  RiskAuditQueryParams,
  RiskAuditRequest,
  SmsVerifyRequest,
  RiskLog,
  RiskLogQueryParams,
  DashboardStats,
  RiskRuleListResult,
  RiskListListResult,
  RiskDeviceListResult,
  RiskAuditListResult,
  RiskLogListResult,
} from '@/types/risk';

export const riskApi = {
  list: (params: RiskQueryParams) => {
    return request.get<RiskEventListResult>('/api/risk/log/list', params);
  },

  detail: (id: string) => {
    return request.get<RiskEvent>(`/api/risk/log/${id}`);
  },

  check: (data: { merchantNo?: string; ip?: string; amount?: number; payMethod?: string }) => {
    return request.post<{ pass: boolean; riskLevel: string; reason?: string }>('/api/risk/check', data);
  },

  handle: (id: string, data: { handleRemark?: string; action: 'pass' | 'block' | 'review' }) => {
    return request.post<void>(`/api/risk/event/${id}/handle`, data);
  },

  ignore: (id: string, reason?: string) => {
    return request.post<void>(`/api/risk/event/${id}/ignore`, { reason });
  },

  getDashboardStats: () => {
    return request.get<RiskDashboardStats>('/api/risk/dashboard');
  },

  addToBlacklist: (data: { type: 'ip' | 'merchant' | 'user'; value: string; reason?: string }) => {
    return request.post<void>('/api/risk/blacklist/add', data);
  },

  removeFromBlacklist: (id: string) => {
    return request.delete<void>(`/api/risk/blacklist/${id}`);
  },

  getBlacklist: (params: RiskQueryParams) => {
    return request.get<RiskEventListResult>('/api/risk/blacklist/list', params);
  },

  getApiStats: (params?: { merchantNo?: string; startTime?: string; endTime?: string }) => {
    return request.get<unknown>('/api/api-stats/summary', params);
  },

  getApiCallTrend: (days: number = 7) => {
    return request.get<{ date: string; count: number }[]>('/api/api-stats/trend', { days });
  },

  listRules: (params: RiskRuleQueryParams) => {
    return request.get<RiskRuleListResult>('/api/risk/rules/page', params);
  },

  getRule: (id: number) => {
    return request.get<RiskRule>(`/api/risk/rules/${id}`);
  },

  createRule: (data: RiskRuleSaveRequest) => {
    return request.post<void>('/api/risk/rules/', data);
  },

  updateRule: (id: number, data: RiskRuleSaveRequest) => {
    return request.put<void>(`/api/risk/rules/${id}`, data);
  },

  deleteRule: (id: number) => {
    return request.delete<void>(`/api/risk/rules/${id}`);
  },

  enableRule: (id: number) => {
    return request.post<void>(`/api/risk/rules/${id}/enable`);
  },

  disableRule: (id: number) => {
    return request.post<void>(`/api/risk/rules/${id}/disable`);
  },

  reloadRules: () => {
    return request.post<void>('/api/risk/rules/reload');
  },

  previewRule: (id: number) => {
    return request.get<string>(`/api/risk/rules/${id}/preview`);
  },

  listBlacklist: (params: RiskListQueryParams) => {
    return request.get<RiskListListResult>('/api/risk/lists/blacklist/page', params);
  },

  addBlacklist: (data: RiskListSaveRequest) => {
    return request.post<void>('/api/risk/lists/blacklist', data);
  },

  deleteBlacklist: (id: number) => {
    return request.delete<void>(`/api/risk/lists/blacklist/${id}`);
  },

  checkBlacklist: (listType: ListType, listValue: string) => {
    return request.get<boolean>('/api/risk/lists/blacklist/check', { listType, listValue });
  },

  listWhitelist: (params: RiskListQueryParams) => {
    return request.get<RiskListListResult>('/api/risk/lists/whitelist/page', params);
  },

  addWhitelist: (data: RiskListSaveRequest) => {
    return request.post<void>('/api/risk/lists/whitelist', data);
  },

  deleteWhitelist: (id: number) => {
    return request.delete<void>(`/api/risk/lists/whitelist/${id}`);
  },

  checkWhitelist: (listType: ListType, listValue: string, ruleCode?: string) => {
    return request.get<boolean>('/api/risk/lists/whitelist/check', { listType, listValue, ruleCode });
  },

  listDevices: (params: RiskDeviceQueryParams) => {
    return request.get<RiskDeviceListResult>('/api/risk/devices/page', params);
  },

  getDevice: (deviceId: string) => {
    return request.get<RiskDevice>(`/api/risk/devices/${deviceId}`);
  },

  updateDeviceRisk: (deviceId: string, scoreDelta: number, tag?: string) => {
    return request.post<void>(`/api/risk/devices/${deviceId}/risk`, { scoreDelta, tag });
  },

  markDeviceAbnormal: (deviceId: string, reason?: string) => {
    return request.post<void>(`/api/risk/devices/${deviceId}/mark-abnormal`, { reason });
  },

  markDeviceNormal: (deviceId: string) => {
    return request.post<void>(`/api/risk/devices/${deviceId}/mark-normal`);
  },

  listAudits: (params: RiskAuditQueryParams) => {
    return request.get<RiskAuditListResult>('/api/risk/audits/page', params);
  },

  getAudit: (id: number) => {
    return request.get<RiskAudit>(`/api/risk/audits/${id}`);
  },

  doAudit: (data: RiskAuditRequest) => {
    return request.post<RiskAudit>('/api/risk/audits/audit', data);
  },

  sendAuditSms: (id: number, mobile: string) => {
    return request.post<string>(`/api/risk/audits/${id}/sms/send`, { mobile });
  },

  verifyAuditSms: (data: SmsVerifyRequest) => {
    return request.post<boolean>('/api/risk/audits/sms/verify', data);
  },

  listRiskLogs: (params: RiskLogQueryParams) => {
    return request.get<RiskLogListResult>('/api/risk/logs/page', params);
  },

  getRiskLog: (id: number) => {
    return request.get<RiskLog>(`/api/risk/logs/${id}`);
  },

  getRiskDashboardStats: () => {
    return request.get<DashboardStats>('/api/risk/dashboard');
  },
};
