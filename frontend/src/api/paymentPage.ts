import { request } from '@/utils/request';
import type {
  PaymentPageConfig,
  PaymentPageConfigSaveRequest,
  PaymentPageQueryParams,
  PaymentPageConfigListResult,
} from '@/types/paymentPage';

export interface UploadResult {
  url: string;
  fileName: string;
  originalName: string;
  size: string;
}

export const paymentPageApi = {
  saveConfig: (data: PaymentPageConfigSaveRequest) => {
    return request.post<PaymentPageConfig>('/payment-page/config', data);
  },

  getByMerchantNo: (merchantNo: string) => {
    return request.get<PaymentPageConfig>(`/payment-page/config/merchant/${merchantNo}`);
  },

  getById: (id: number) => {
    return request.get<PaymentPageConfig>(`/payment-page/config/${id}`);
  },

  list: (params: PaymentPageQueryParams) => {
    return request.get<PaymentPageConfigListResult>('/payment-page/config/list', params);
  },

  updateStatus: (id: number, status: number) => {
    return request.put<void>(`/payment-page/config/${id}/status`, null, { params: { status } });
  },

  deleteConfig: (id: number) => {
    return request.delete<void>(`/payment-page/config/${id}`);
  },

  getPublicConfig: (merchantNo: string) => {
    return request.get<PaymentPageConfig>(`/payment-page/public/${merchantNo}`);
  },

  uploadImage: (file: File, bizType = 'payment-page') => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('bizType', bizType);
    return request.post<UploadResult>('/file/upload/image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },
};

export const colorSchemes = [
  {
    code: 'DEFAULT',
    name: '简约经典',
    description: '简洁大方的经典设计，适合大多数场景',
    primaryColor: '#1677ff',
    backgroundColor: '#f5f7fa',
    buttonColor: '#1677ff',
  },
  {
    code: 'ELEGANT',
    name: '优雅商务',
    description: '深色主题，高端商务感',
    primaryColor: '#1a1a2e',
    backgroundColor: '#0f0f1a',
    buttonColor: '#e94560',
  },
  {
    code: 'FRESH',
    name: '清新活力',
    description: '明亮清新，充满活力',
    primaryColor: '#00b894',
    backgroundColor: '#e8f8f5',
    buttonColor: '#00b894',
  },
  {
    code: 'WARM',
    name: '温暖橙调',
    description: '温暖亲和，提升信任感',
    primaryColor: '#f39c12',
    backgroundColor: '#fdf6e3',
    buttonColor: '#e67e22',
  },
  {
    code: 'TECH',
    name: '科技紫',
    description: '科技感十足，适合互联网产品',
    primaryColor: '#6c5ce7',
    backgroundColor: '#f0edfc',
    buttonColor: '#6c5ce7',
  },
  {
    code: 'NATURE',
    name: '自然绿',
    description: '自然舒适，环保健康',
    primaryColor: '#27ae60',
    backgroundColor: '#eafaf1',
    buttonColor: '#27ae60',
  },
];
