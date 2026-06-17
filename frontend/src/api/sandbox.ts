import { request } from '@/utils/request';
import type { SandboxTest, SandboxQueryParams, SandboxListResult, SandboxExecuteRequest } from '@/types/sandbox';

export const sandboxApi = {
  list: (params: SandboxQueryParams) => {
    return request.get<SandboxListResult>('/api/sandbox/test/list', params);
  },

  detail: (id: string) => {
    return request.get<SandboxTest>(`/api/sandbox/test/${id}`);
  },

  getScenes: () => {
    return request.get<{ code: string; name: string; description?: string }[]>('/api/sandbox/test/scenes');
  },

  execute: (data: SandboxExecuteRequest) => {
    return request.post<{ testId: string; status: string; result?: unknown }>('/api/sandbox/test/execute', data);
  },

  getMerchants: () => {
    return request.get<{ id: string; merchantName: string }[]>('/api/sandbox/merchants');
  },

  getPayMethods: () => {
    return request.get<{ code: string; name: string; channels: { code: string; name: string }[] }[]>('/api/sandbox/pay-methods');
  },

  clearHistory: () => {
    return request.post<void>('/api/sandbox/clear-history');
  },
};
