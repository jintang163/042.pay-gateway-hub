package com.payhub.merchant.service;

import com.payhub.merchant.dto.BusinessInfoDTO;
import com.payhub.merchant.entity.MerchantInfo;

public interface MerchantAutoAuditService {

    void triggerAutoAudit(String merchantNo);

    BusinessInfoDTO verifyBusinessLicense(MerchantInfo merchant);

    Integer evaluateRisk(MerchantInfo merchant, BusinessInfoDTO businessInfo);

    boolean executeAutoAudit(MerchantInfo merchant);
}
