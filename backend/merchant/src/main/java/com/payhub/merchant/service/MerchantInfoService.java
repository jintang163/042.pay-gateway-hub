package com.payhub.merchant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.merchant.dto.*;
import com.payhub.merchant.entity.MerchantInfo;

import java.util.Map;

public interface MerchantInfoService extends IService<MerchantInfo> {

    MerchantApplyResult apply(MerchantApplyRequest request);

    void audit(MerchantAuditRequest request);

    MerchantVO getByMerchantNo(String merchantNo);

    IPage<MerchantVO> listPage(Long current, Long size, Map<String, Object> params);

    ApiKeyVO resetApiKey(ApiKeyResetRequest request);

    boolean testCallback(String merchantNo, String url);

    AuditProgressVO getAuditProgress(String merchantNo);

    Map<String, Integer> getManualAuditStats();
}
