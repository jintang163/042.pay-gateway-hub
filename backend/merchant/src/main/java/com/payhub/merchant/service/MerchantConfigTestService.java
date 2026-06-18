package com.payhub.merchant.service;

import com.payhub.merchant.dto.MerchantConfigTestReport;
import com.payhub.merchant.dto.MerchantConfigTestRequest;

public interface MerchantConfigTestService {

    MerchantConfigTestReport runTest(MerchantConfigTestRequest request);
}
