import { request } from '@/utils/request';
import type {
  PayLink,
  PayLinkSaveRequest,
  PayLinkQueryParams,
  PayLinkPageResult,
  Coupon,
  CouponSaveRequest,
  CouponQueryParams,
  CouponPageResult,
  CouponDiscountCalcRequest,
  CouponDiscountCalcResult,
  Activity,
  ActivitySaveRequest,
  ActivityQueryParams,
  ActivityPageResult,
} from '@/types/marketing';

export const payLinkApi = {
  list: (params: PayLinkQueryParams) => {
    return request.get<PayLinkPageResult>('/pay-link/list', params);
  },

  detail: (linkCode: string) => {
    return request.get<PayLink>(`/pay-link/${linkCode}`);
  },

  save: (data: PayLinkSaveRequest) => {
    return request.post<void>('/pay-link/save', data);
  },

  toggle: (id: number) => {
    return request.post<void>(`/pay-link/${id}/toggle`);
  },

  remove: (id: number) => {
    return request.delete<void>(`/pay-link/${id}`);
  },

  resolve: (linkCode: string) => {
    return request.get<PayLink>(`/pay-link/resolve/${linkCode}`);
  },
};

export const couponApi = {
  list: (params: CouponQueryParams) => {
    return request.get<CouponPageResult>('/coupon/list', params);
  },

  detail: (couponCode: string) => {
    return request.get<Coupon>(`/coupon/${couponCode}`);
  },

  save: (data: CouponSaveRequest) => {
    return request.post<void>('/coupon/save', data);
  },

  toggle: (id: number) => {
    return request.post<void>(`/coupon/${id}/toggle`);
  },

  remove: (id: number) => {
    return request.delete<void>(`/coupon/${id}`);
  },

  calculateDiscount: (data: CouponDiscountCalcRequest) => {
    return request.post<CouponDiscountCalcResult>('/coupon/calculate-discount', data);
  },
};

export const activityApi = {
  list: (params: ActivityQueryParams) => {
    return request.get<ActivityPageResult>('/activity/list', params);
  },

  detail: (activityCode: string) => {
    return request.get<Activity>(`/activity/${activityCode}`);
  },

  save: (data: ActivitySaveRequest) => {
    return request.post<void>('/activity/save', data);
  },

  toggle: (id: number) => {
    return request.post<void>(`/activity/${id}/toggle`);
  },

  remove: (id: number) => {
    return request.delete<void>(`/activity/${id}`);
  },
};
