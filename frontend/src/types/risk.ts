import type { PageParams, PageResult } from './common';

export type RiskLevel = 'low' | 'medium' | 'high' | 'critical';
export type RiskStatus = 'pending' | 'handled' | 'ignored';
export type RiskType = 'fraud' | 'abnormal' | 'limit_exceed' | 'blacklist' | 'other';

export interface RiskEvent {
  id: string;
  eventId: string;
  merchantId?: string;
  merchantName?: string;
  orderId?: string;
  orderNo?: string;
  type: RiskType;
  level: RiskLevel;
  title: string;
  description?: string;
  evidence?: Record<string, unknown>;
  status: RiskStatus;
  handler?: string;
  handleTime?: string;
  handleRemark?: string;
  createTime: string;
  updateTime?: string;
}

export interface RiskQueryParams extends PageParams {
  eventId?: string;
  merchantId?: string;
  orderNo?: string;
  type?: RiskType;
  level?: RiskLevel;
  status?: RiskStatus;
  startTime?: string;
  endTime?: string;
}

export type RiskEventListResult = PageResult<RiskEvent>;

export interface RiskDashboardStats {
  totalEvents: number;
  pendingEvents: number;
  highRiskEvents: number;
  todayEvents: number;
  eventTrend: { date: string; count: number }[];
  riskLevelDistribution: { name: string; value: number }[];
  riskTypeDistribution: { name: string; value: number }[];
}

export type RuleType = 'AMOUNT' | 'FREQUENCY' | 'IP_BLACKLIST' | 'DEVICE' | 'BEHAVIOR';
export type ActionType = 'PASS' | 'BLOCK' | 'SMS' | 'MANUAL';

export interface RiskRule {
  id?: number;
  ruleCode: string;
  ruleName: string;
  ruleType: RuleType;
  riskLevel: 1 | 2 | 3;
  ruleCondition: string;
  ruleContent?: string;
  actionType: ActionType;
  smsTemplateId?: string;
  priority?: number;
  status: 0 | 1;
  effectStartTime?: string;
  effectEndTime?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface RiskRuleQueryParams extends PageParams {
  ruleType?: RuleType;
  riskLevel?: 1 | 2 | 3;
  status?: 0 | 1;
  ruleName?: string;
}

export interface RiskRuleSaveRequest {
  ruleCode: string;
  ruleName: string;
  ruleType: RuleType;
  riskLevel: 1 | 2 | 3;
  ruleCondition: string;
  ruleContent?: string;
  actionType: ActionType;
  smsTemplateId?: string;
  priority?: number;
  status: 0 | 1;
  effectStartTime?: string;
  effectEndTime?: string;
  remark?: string;
}

export type ListType = 'IP' | 'USER' | 'MERCHANT' | 'DEVICE';
export type ListSource = 'MANUAL' | 'SYSTEM' | 'AUDIT' | 'RISK';

export interface RiskList {
  id?: number;
  listType: ListType;
  listValue: string;
  listSource?: ListSource;
  riskLevel?: 1 | 2 | 3;
  bypassRules?: string;
  reason?: string;
  operatorId?: string;
  operatorName?: string;
  status: 0 | 1;
  statusDesc?: string;
  expireTime?: string;
  hitCount?: number;
  lastHitTime?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface RiskListQueryParams extends PageParams {
  listType?: ListType;
  listValue?: string;
  status?: 0 | 1;
}

export interface RiskListSaveRequest {
  listType: ListType;
  listValue: string;
  listSource?: ListSource;
  riskLevel?: 1 | 2 | 3;
  bypassRules?: string;
  reason?: string;
  expireTime?: string;
}

export interface RiskDevice {
  id?: number;
  deviceId: string;
  deviceType?: string;
  osType?: string;
  osVersion?: string;
  browserType?: string;
  browserVersion?: string;
  appVersion?: string;
  screenResolution?: string;
  language?: string;
  timezone?: string;
  userAgent?: string;
  userIdentity?: string;
  merchantNo?: string;
  firstSeenIp?: string;
  lastSeenIp?: string;
  firstSeenTime?: string;
  lastSeenTime?: string;
  totalRequestCount?: number;
  riskScore: number;
  riskTags?: string;
  status: 0 | 1;
  statusDesc?: string;
  extraInfo?: Record<string, unknown>;
  createdAt?: string;
  updatedAt?: string;
}

export interface RiskDeviceQueryParams extends PageParams {
  deviceId?: string;
  userIdentity?: string;
  merchantNo?: string;
  status?: 0 | 1;
  riskScoreMin?: number;
}

export type AuditStatus = 0 | 1 | 2 | 3;
export type AuditType = 'MANUAL' | 'SMS' | 'BLOCK';

export interface RiskAudit {
  id?: number;
  auditNo: string;
  riskLogId: number;
  merchantNo?: string;
  orderNo?: string;
  auditType: AuditType;
  auditLevel?: number;
  auditStatus: AuditStatus;
  riskLevelBefore?: number;
  riskLevelAfter?: number;
  auditResult?: 'APPROVED' | 'REJECTED';
  auditRemark?: string;
  auditUserId?: string;
  auditUserName?: string;
  auditTime?: string;
  smsVerified?: 0 | 1;
  smsMobile?: string;
  smsVerifyTime?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface RiskAuditQueryParams extends PageParams {
  auditNo?: string;
  auditStatus?: AuditStatus;
  auditType?: AuditType;
  merchantNo?: string;
  orderNo?: string;
}

export interface RiskAuditRequest {
  riskLogId: number;
  auditResult: 'APPROVED' | 'REJECTED';
  auditRemark?: string;
}

export interface SmsVerifyRequest {
  mobile: string;
  code: string;
  riskLogId: number;
}

export interface RiskLog {
  id: number;
  merchantNo: string;
  orderNo?: string;
  riskType: string;
  riskLevel: 0 | 1 | 2 | 3;
  riskRule?: string;
  riskDesc?: string;
  clientIp?: string;
  userIdentity?: string;
  deviceId?: string;
  payAmount?: number;
  payChannel?: string;
  requestParams?: string;
  actionType?: ActionType;
  handleResult?: 0 | 1 | 2;
  handleDesc?: string;
  auditStatus: 0 | 1 | 2 | 3;
  auditStatusDesc?: string;
  auditId?: number;
  triggerTime?: string;
  createdAt?: string;
}

export interface RiskLogQueryParams extends PageParams {
  merchantNo?: string;
  orderNo?: string;
  riskType?: string;
  riskLevel?: 0 | 1 | 2 | 3;
  actionType?: ActionType;
  auditStatus?: 0 | 1 | 2 | 3;
  clientIp?: string;
  startTime?: string;
  endTime?: string;
}

export interface DashboardStats {
  todayTotal: number;
  todayPassed: number;
  todayBlocked: number;
  todayHighRisk: number;
  todayMediumRisk: number;
  todayLowRisk: number;
  todayBlockRate: string;
  pendingAuditCount: number;
  weekTrend: Record<string, { total: number; blocked: number }>;
  riskTypeDistribution: Record<string, number>;
}

export type RiskRuleListResult = PageResult<RiskRule>;
export type RiskListListResult = PageResult<RiskList>;
export type RiskDeviceListResult = PageResult<RiskDevice>;
export type RiskAuditListResult = PageResult<RiskAudit>;
export type RiskLogListResult = PageResult<RiskLog>;
