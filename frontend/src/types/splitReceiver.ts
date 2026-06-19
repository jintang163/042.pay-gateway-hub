export interface IPageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}

export interface SplitReceiver {
  id: number;
  receiverNo: string;
  merchantNo: string;
  receiverName: string;
  receiverType: number;
  idCardNo: string;
  idCardName: string;
  bankCardNo: string;
  bankPhone: string;
  bankName: string;
  bankBranchName: string;
  verifyStatus: number;
  verifyChannel: string;
  verifyTime: string;
  verifyFailReason: string;
  verifyRequestId: string;
  contactName: string;
  contactPhone: string;
  contactEmail: string;
  status: number;
  remark: string;
  operatorId: string;
  operatorName: string;
  createdAt: string;
  updatedAt: string;
  verifyStatusDesc: string;
  receiverTypeDesc: string;
  verifyChannelDesc: string;
  statusDesc: string;
}

export interface SplitReceiverSaveRequest {
  receiverNo?: string;
  receiverName: string;
  receiverType: number;
  idCardNo: string;
  idCardName: string;
  bankCardNo: string;
  bankPhone: string;
  bankName: string;
  bankBranchName?: string;
  contactName?: string;
  contactPhone?: string;
  contactEmail?: string;
  remark?: string;
  status?: number;
}

export interface SplitReceiverVerifyRequest {
  receiverNo: string;
  verifyChannel: string;
}

export interface SplitReceiverBatchImportItem {
  receiverName: string;
  receiverType: number;
  idCardNo: string;
  idCardName: string;
  bankCardNo: string;
  bankPhone: string;
  bankName: string;
  bankBranchName?: string;
  contactName?: string;
  contactPhone?: string;
  contactEmail?: string;
  remark?: string;
}

export interface SplitReceiverVerifyLog {
  id: number;
  logNo: string;
  merchantNo: string;
  receiverNo: string;
  verifyChannel: string;
  verifyRequestId: string;
  idCardName: string;
  idCardNo: string;
  bankCardNo: string;
  bankPhone: string;
  verifyStatus: number;
  verifyResult: string;
  verifyFailCode: string;
  verifyFailReason: string;
  verifyTime: string;
  requestData: string;
  responseData: string;
  operatorId: string;
  operatorName: string;
  createdAt: string;
  verifyChannelDesc: string;
  verifyStatusDesc: string;
}

export interface SplitReceiverQueryParams {
  current?: number;
  size?: number;
  receiverNo?: string;
  receiverName?: string;
  receiverType?: number;
  verifyStatus?: number;
  status?: number;
  idCardNo?: string;
  bankCardNo?: string;
}

export interface SplitReceiverVerifyLogQueryParams {
  current?: number;
  size?: number;
  receiverNo?: string;
  verifyChannel?: string;
  verifyStatus?: number;
  verifyRequestId?: string;
}
