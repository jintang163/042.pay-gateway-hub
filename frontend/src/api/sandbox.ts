import { request } from '@/utils/request';
import type {
  SandboxTest,
  SandboxQueryParams,
  SandboxListResult,
  SandboxExecuteRequest,
  SandboxSceneOption,
  SandboxMerchant,
  SandboxPayMethod,
  SandboxTestResult,
} from '@/types/sandbox';

export const sandboxApi = {
  getStatus: () => {
    return request.get<{ enabled: boolean; description: string; dataRetentionDays: number; cleanTime: string }>(
      '/api/sandbox/status'
    );
  },

  list: (params: SandboxQueryParams) => {
    return request.get<SandboxListResult>('/api/sandbox/test/records', params);
  },

  detail: (testId: string) => {
    return request.get<SandboxTestResult>(`/api/sandbox/test/records/${testId}`);
  },

  getScenes: () => {
    return request.get<SandboxSceneOption[]>('/api/sandbox/test/scenes');
  },

  execute: (data: SandboxExecuteRequest) => {
    return request.post<SandboxTestResult>('/api/sandbox/test', data);
  },

  getMerchants: () => {
    return request.get<SandboxMerchant[]>('/api/sandbox/merchants');
  },

  getPayMethods: () => {
    return request.get<SandboxPayMethod[]>('/api/sandbox/pay-methods');
  },

  clearHistory: () => {
    return request.post<void>('/api/sandbox/clear-history');
  },
};
