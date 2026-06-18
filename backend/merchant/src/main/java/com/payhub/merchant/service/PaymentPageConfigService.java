package com.payhub.merchant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.merchant.dto.PaymentPageConfigSaveRequest;
import com.payhub.merchant.dto.PaymentPageConfigVO;
import com.payhub.merchant.entity.PaymentPageConfig;

import java.util.Map;

public interface PaymentPageConfigService extends IService<PaymentPageConfig> {

    PaymentPageConfigVO saveConfig(PaymentPageConfigSaveRequest request);

    PaymentPageConfigVO getByMerchantNo(String merchantNo);

    PaymentPageConfigVO getById(Long id);

    IPage<PaymentPageConfigVO> listPage(Long current, Long size, Map<String, Object> params);

    void updateStatus(Long id, Integer status);

    void deleteConfig(Long id);

    PaymentPageConfigVO getPublicConfig(String merchantNo);
}
