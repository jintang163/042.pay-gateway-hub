import { request } from '@/utils/request';
import type {
  Invoice,
  InvoiceApplyRequest,
  InvoiceRedFlushRequest,
  InvoiceQueryParams,
  InvoiceListResult,
} from '@/types/invoice';

export const invoiceApi = {
  apply: (data: InvoiceApplyRequest) => {
    return request.post<Invoice>('/api/invoice/apply', data);
  },

  redFlush: (data: InvoiceRedFlushRequest) => {
    return request.post<Invoice>('/api/invoice/red-flush', data);
  },

  batchApply: (data: InvoiceApplyRequest[]) => {
    return request.post<Invoice[]>('/api/invoice/batch-apply', data);
  },

  list: (params: InvoiceQueryParams) => {
    return request.get<InvoiceListResult>('/api/invoice/list', params);
  },

  detail: (invoiceNo: string) => {
    return request.get<Invoice>(`/api/invoice/${invoiceNo}`);
  },

  queryStatus: (invoiceNo: string) => {
    return request.get<Invoice>(`/api/invoice/${invoiceNo}/status`);
  },

  downloadPdf: (invoiceNo: string) => {
    return request.download(`/api/invoice/${invoiceNo}/download`);
  },

  getPdfUrl: (invoiceNo: string) => {
    return request.get<string>(`/api/invoice/${invoiceNo}/pdf-url`);
  },
};
