package com.payhub.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.settlement.dto.ReportSubscriptionSaveRequest;
import com.payhub.settlement.dto.ReportSubscriptionVO;
import com.payhub.settlement.entity.ReportSubscription;

import java.util.Map;

public interface ReportSubscriptionService extends IService<ReportSubscription> {

    IPage<ReportSubscriptionVO> listPage(Long current, Long size, Map<String, Object> params);

    ReportSubscriptionVO getSubscriptionById(Long id);

    void saveSubscription(ReportSubscriptionSaveRequest request);

    void updateSubscription(Long id, ReportSubscriptionSaveRequest request);

    void deleteSubscription(Long id);

    void toggleSubscription(Long id);

    boolean validateOwnership(Long id, String merchantNo);
}
