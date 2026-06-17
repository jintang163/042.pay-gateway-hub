package com.payhub.pay.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.pay.dto.PayConfigSaveRequest;
import com.payhub.pay.dto.PayConfigVO;
import com.payhub.pay.entity.MerchantPayConfig;

import java.util.List;
import java.util.Map;

public interface MerchantPayConfigService extends IService<MerchantPayConfig> {

    void saveConfig(PayConfigSaveRequest request);

    void deleteConfig(Long id);

    PayConfigVO getConfigById(Long id);

    List<PayConfigVO> listByMerchantNo(String merchantNo);

    IPage<PayConfigVO> listPage(Long current, Long size, Map<String, Object> params);

    void toggleConfig(Long id);
}
