export interface PayLink {
  id: number;
  linkCode: string;
  merchantNo: string;
  title: string;
  fixedAmount?: number;
  amountEditable: boolean;
  minAmount?: number;
  maxAmount?: number;
  payChannel?: string;
  productSubject?: string;
  productDetail?: string;
  notifyUrl?: string;
  redirectUrl?: string;
  expireTime?: string;
  singleUse: boolean;
  maxUseCount?: number;
  usedCount: number;
  status: number;
  statusDesc?: string;
  remark?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface PayLinkSaveRequest {
  id?: number;
  merchantNo: string;
  title: string;
  fixedAmount?: number;
  amountEditable?: boolean;
  minAmount?: number;
  maxAmount?: number;
  payChannel?: string;
  productSubject?: string;
  productDetail?: string;
  notifyUrl?: string;
  redirectUrl?: string;
  expireTime?: string;
  singleUse?: boolean;
  maxUseCount?: number;
  remark?: string;
}

export interface PayLinkQueryParams {
  current?: number;
  size?: number;
  linkCode?: string;
  title?: string;
  status?: number;
  startTime?: string;
  endTime?: string;
}

export interface PayLinkPageResult {
  records: PayLink[];
  total: number;
  current: number;
  size: number;
  pages: number;
}

export interface Coupon {
  id: number;
  couponCode: string;
  merchantNo: string;
  couponName: string;
  couponType: number;
  couponTypeDesc?: string;
  discountValue: number;
  minOrderAmount?: number;
  maxDiscount?: number;
  totalQuantity: number;
  issuedCount: number;
  usedCount: number;
  startTime?: string;
  endTime?: string;
  status: number;
  statusDesc?: string;
  remark?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface CouponSaveRequest {
  id?: number;
  merchantNo: string;
  couponName: string;
  couponType: number;
  discountValue: number;
  minOrderAmount?: number;
  maxDiscount?: number;
  totalQuantity: number;
  startTime?: string;
  endTime?: string;
  remark?: string;
}

export interface CouponQueryParams {
  current?: number;
  size?: number;
  couponCode?: string;
  couponName?: string;
  couponType?: number;
  status?: number;
}

export interface CouponPageResult {
  records: Coupon[];
  total: number;
  current: number;
  size: number;
  pages: number;
}

export interface CouponDiscountCalcRequest {
  couponCode: string;
  orderAmount: number;
  merchantNo?: string;
}

export interface CouponDiscountCalcResult {
  couponCode: string;
  couponName: string;
  couponType: number;
  couponTypeDesc?: string;
  orderAmount: number;
  discountAmount: number;
  actualAmount: number;
  calcDetail: string;
}

export interface Activity {
  id: number;
  activityCode: string;
  merchantNo: string;
  activityName: string;
  activityType: number;
  activityTypeDesc?: string;
  thresholdAmount?: number;
  discountAmount?: number;
  discountRate?: number;
  maxDiscount?: number;
  startTime?: string;
  endTime?: string;
  status: number;
  statusDesc?: string;
  remark?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface ActivitySaveRequest {
  id?: number;
  merchantNo: string;
  activityName: string;
  activityType: number;
  thresholdAmount?: number;
  discountAmount?: number;
  discountRate?: number;
  maxDiscount?: number;
  startTime?: string;
  endTime?: string;
  remark?: string;
}

export interface ActivityQueryParams {
  current?: number;
  size?: number;
  activityCode?: string;
  activityName?: string;
  activityType?: number;
  status?: number;
}

export interface ActivityPageResult {
  records: Activity[];
  total: number;
  current: number;
  size: number;
  pages: number;
}
