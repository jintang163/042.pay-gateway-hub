import { request } from '@/utils/request';
import type {
  FeeRule,
  FeeRuleQueryParams,
  FeeRulePageResult,
  FeeRuleSaveRequest,
  FeeCalcRequest,
  FeeCalcResult,
  IndustryOption,
} from '@/types/feeRule';

export const feeRuleApi = {
  list: (params: FeeRuleQueryParams) => {
    return request.get<FeeRulePageResult>('/fee-rule/list', params);
  },

  listByIndustry: (industryCode: string, payChannel?: string) => {
    return request.get<FeeRule[]>('/fee-rule/list-by-industry', { industryCode, payChannel });
  },

  industries: () => {
    return request.get<IndustryOption[]>('/fee-rule/industries');
  },

  detail: (ruleNo: string) => {
    return request.get<FeeRule>(`/fee-rule/${ruleNo}`);
  },

  save: (data: FeeRuleSaveRequest) => {
    return request.post<void>('/fee-rule/save', data);
  },

  toggle: (id: number) => {
    return request.post<void>(`/fee-rule/${id}/toggle`);
  },

  remove: (id: number) => {
    return request.delete<void>(`/fee-rule/${id}`);
  },

  calculate: (data: FeeCalcRequest) => {
    return request.post<FeeCalcResult>('/fee-rule/calculate', data);
  },
};
