import { request } from '@/utils/request';
import type {
  SplitReceiver,
  SplitReceiverSaveRequest,
  SplitReceiverVerifyRequest,
  SplitReceiverBatchImportItem,
  SplitReceiverVerifyLog,
  SplitReceiverQueryParams,
  SplitReceiverVerifyLogQueryParams,
  IPageResult,
} from '@/types/splitReceiver';

export const splitReceiverApi = {
  list: (params: SplitReceiverQueryParams) => {
    return request.get<IPageResult<SplitReceiver>>('/split-receiver/list', params);
  },

  detail: (receiverNo: string) => {
    return request.get<SplitReceiver>(`/split-receiver/${receiverNo}`);
  },

  save: (data: SplitReceiverSaveRequest) => {
    return request.post<void>('/split-receiver/save', data);
  },

  toggle: (id: string | number) => {
    return request.post<void>(`/split-receiver/${id}/toggle`);
  },

  remove: (id: string | number) => {
    return request.delete<void>(`/split-receiver/${id}`);
  },

  verify: (data: SplitReceiverVerifyRequest) => {
    return request.post<void>('/split-receiver/verify', data);
  },

  available: () => {
    return request.get<SplitReceiver[]>('/split-receiver/available');
  },

  batchImport: (items: SplitReceiverBatchImportItem[]) => {
    return request.post<void>('/split-receiver/batch-import', items);
  },

  batchImportFile: (file: File, autoVerify: boolean) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('autoVerify', String(autoVerify));
    return request.upload<any>('/split-receiver/batch-import-file', formData);
  },

  batchVerify: (receiverNos: string[]) => {
    return request.post<any>('/split-receiver/batch-verify', { receiverNos });
  },

  verifyLogs: (params: SplitReceiverVerifyLogQueryParams) => {
    return request.get<IPageResult<SplitReceiverVerifyLog>>('/split-receiver/verify-logs', params);
  },
};
