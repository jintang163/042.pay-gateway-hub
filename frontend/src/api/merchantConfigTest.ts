import { request } from '@/utils/request';
import type {
  MerchantConfigTestReport,
  MerchantConfigTestRequest,
} from '@/types/merchantConfigTest';

export const merchantConfigTestApi = {
  runTest: (data: MerchantConfigTestRequest) => {
    return request.post<MerchantConfigTestReport>('/merchant-config/test', data);
  },

  quickTest: (merchantNo: string) => {
    return request.get<MerchantConfigTestReport>(`/merchant-config/test/${merchantNo}`);
  },
};
