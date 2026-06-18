import { request } from '@/utils/request';
import type {
  CallbackSimulateLog,
  CallbackSimulateRequest,
  CallbackSimulateQueryParams,
  CallbackSimulateListResult,
  CallbackResendRequest,
  SignCodeExampleRequest,
  SignCodeExample,
} from '@/types/callbackSimulate';

export const callbackSimulateApi = {
  send: (data: CallbackSimulateRequest) => {
    return request.post<CallbackSimulateLog>('/callback-simulate/send', data);
  },

  list: (params: CallbackSimulateQueryParams) => {
    return request.get<CallbackSimulateListResult>('/callback-simulate/list', params);
  },

  detail: (logNo: string) => {
    return request.get<CallbackSimulateLog>(`/callback-simulate/${logNo}`);
  },

  resend: (data: CallbackResendRequest) => {
    return request.post<CallbackSimulateLog>('/callback-simulate/resend', data);
  },

  generateSignCode: (data: SignCodeExampleRequest) => {
    return request.post<SignCodeExample>('/callback-simulate/sign-code', data);
  },
};
