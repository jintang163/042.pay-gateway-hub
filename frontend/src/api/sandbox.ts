import { request } from '@/utils/request';
import type { SandboxTest, SandboxQueryParams, SandboxListResult, SandboxExecuteRequest } from '@/types/sandbox';

export const sandboxApi = {
  list: (params: SandboxQueryParams) => {
    return request.get<SandboxListResult>('/sandbox/test/list', params);
  },

  detail: (id: string) => {
    return request.get<SandboxTest>(`/sandbox/test/${id}`);
  },

  execute: (data: SandboxExecuteRequest) => {
    return request.post<{ testId: string; status: string; result?: unknown }>('/sandbox/execute', data);
  },

  getMerchants: () => {
    return request.get<{ id: string; merchantName: string }[]>('/sandbox/merchants');
  },

  getPayMethods: () => {
    return request.get<{ code: string; name: string; channels: { code: string; name: string }[] }[]>('/sandbox/pay-methods');
  },

  clearHistory: () => {
    return request.post<void>('/sandbox/clear-history');
  },
};
