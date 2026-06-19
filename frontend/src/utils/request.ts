import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios';
import { message } from 'antd';
import type { ApiResponse } from '@/types/common';
import { useUserStore } from '@/store/userStore';
import { useAppStore } from '@/store/appStore';
import { downloadFile } from '@/utils';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';
const TOKEN_KEY = 'access_token';
const REFRESH_TOKEN_KEY = 'refresh_token';

const service: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

let isRefreshing = false;
let pendingRequests: Array<(token: string) => void> = [];

const addPendingRequest = (callback: (token: string) => void) => {
  pendingRequests.push(callback);
};

const executePendingRequests = (token: string) => {
  pendingRequests.forEach((callback) => callback(token));
  pendingRequests = [];
};

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    config.headers['X-Request-Id'] = `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

    const appStore = useAppStore.getState();
    if (appStore.sandboxMode) {
      config.headers['X-Sandbox-Mode'] = 'true';
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse | Blob>) => {
    if (response.config.responseType === 'blob') {
      return response;
    }
    const res = response.data as ApiResponse;
    if (res.code !== 0 && res.code !== 200) {
      message.error(res.message || '请求失败');
      return Promise.reject(new Error(res.message || 'Error'));
    }
    return res.data as unknown as AxiosResponse;
  },
  async (error) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response) {
      const { status, data } = error.response;

      if (status === 401) {
        if (!originalRequest._retry) {
          originalRequest._retry = true;

          if (isRefreshing) {
            return new Promise((resolve) => {
              addPendingRequest((token: string) => {
                originalRequest.headers.Authorization = `Bearer ${token}`;
                resolve(service(originalRequest));
              });
            });
          }

          isRefreshing = true;
          const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);

          try {
            const response = await axios.post<ApiResponse<{ token: string; refreshToken: string }>>(
              `${BASE_URL}/merchant/user/refresh`,
              { refreshToken }
            );
            const newToken = response.data.data.token;
            const newRefreshToken = response.data.data.refreshToken;

            localStorage.setItem(TOKEN_KEY, newToken);
            localStorage.setItem(REFRESH_TOKEN_KEY, newRefreshToken);

            executePendingRequests(newToken);
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            return service(originalRequest);
          } catch {
            localStorage.removeItem(TOKEN_KEY);
            localStorage.removeItem(REFRESH_TOKEN_KEY);
            const userStore = useUserStore.getState();
            userStore.logout();
            window.location.href = '/login';
            return Promise.reject(error);
          } finally {
            isRefreshing = false;
          }
        }
      } else if (status === 403) {
        message.error('没有权限访问该资源');
      } else if (status === 404) {
        message.error('请求的资源不存在');
      } else if (status >= 500) {
        message.error(data?.message || '服务器内部错误');
      }
    } else if (error.code === 'ECONNABORTED') {
      message.error('请求超时，请稍后重试');
    } else if (error.message.includes('Network Error')) {
      message.error('网络连接失败，请检查网络');
    }

    return Promise.reject(error);
  }
);

export const request = {
  get<T = unknown>(url: string, params?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return service.get(url, { params, ...config });
  },
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config);
  },
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return service.put(url, data, config);
  },
  delete<T = unknown>(url: string, params?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, { params, ...config });
  },
  upload<T = unknown>(url: string, formData: FormData, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      ...config,
    });
  },
  async download(url: string, params?: unknown, config?: AxiosRequestConfig): Promise<void> {
    const response = await service.get(url, {
      params,
      responseType: 'blob',
      ...config,
    });
    const blob = response.data as Blob;
    const disposition = response.headers['content-disposition'];
    let filename = `download_${Date.now()}`;
    if (disposition) {
      const match = disposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
      if (match && match[1]) {
        filename = match[1].replace(/['"]/g, '');
        try {
          filename = decodeURIComponent(filename);
        } catch {
          // ignore decode error
        }
      }
    }
    if (blob.type.includes('text/csv') && !filename.endsWith('.csv')) {
      filename += '.csv';
    }
    downloadFile(blob, filename);
    message.success('文件下载成功');
  },
};

export default service;
