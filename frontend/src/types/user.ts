export interface User {
  id: string;
  username: string;
  nickname?: string;
  avatar?: string;
  email?: string;
  phone?: string;
  role: 'admin' | 'merchant' | 'operator';
  status: 'active' | 'disabled';
  createdAt?: string;
  updatedAt?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
  captcha?: string;
}

export interface LoginResponse {
  token: string;
  refreshToken?: string;
  user: User;
  expiresIn: number;
}

export interface RegisterRequest {
  username: string;
  password: string;
  confirmPassword: string;
  email?: string;
  phone?: string;
}

export interface ResetPasswordRequest {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}
