import { request } from '@/utils/request';
import type { LoginRequest, LoginResponse, ResetPasswordRequest } from '@/types/user';

export const authApi = {
  login: (data: LoginRequest) => {
    return request.post<LoginResponse>('/merchant/user/login', data);
  },

  register: (data: { username: string; password: string; merchantName: string; contact?: string; phone?: string; email?: string }) => {
    return request.post<{ id: string; merchantNo: string }>('/merchant/user/register', data);
  },

  logout: () => {
    return request.post<void>('/merchant/user/logout');
  },

  refreshToken: (refreshToken: string) => {
    return request.post<{ token: string; refreshToken: string; expiresIn: number }>('/merchant/user/refresh', { refreshToken });
  },

  getCaptcha: () => {
    return request.get<{ key: string; image: string }>('/merchant/user/captcha');
  },

  resetPassword: (data: ResetPasswordRequest) => {
    return request.post<void>('/merchant/user/reset-password', data);
  },

  getCurrentUser: () => {
    return request.get<{ id: string; username: string; nickname?: string; avatar?: string; role: string }>('/merchant/user/info');
  },
};
