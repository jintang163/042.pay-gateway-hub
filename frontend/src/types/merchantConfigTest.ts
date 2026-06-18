export interface TestItemResult {
  itemCode: string;
  itemName: string;
  itemCategory: string;
  status: 'PASS' | 'FAIL' | 'WARN' | 'ERROR' | 'PENDING';
  statusDesc: string;
  durationMs?: number;
  expectedValue?: string;
  actualValue?: string;
  message?: string;
  detail?: string;
  suggestion?: string;
}

export interface MerchantConfigTestReport {
  merchantNo: string;
  merchantName?: string;
  totalTests: number;
  passedTests: number;
  failedTests: number;
  overallStatus: 'PASS' | 'FAIL' | 'PARTIAL';
  overallStatusDesc: string;
  totalTimeMs: number;
  testTime: string;
  items: TestItemResult[];
  summary: string;
}

export interface MerchantConfigTestRequest {
  merchantNo: string;
  callbackUrl?: string;
  signType?: string;
  testAmount?: string;
}
