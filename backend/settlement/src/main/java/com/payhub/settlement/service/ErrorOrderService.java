package com.payhub.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.settlement.dto.ErrorOrderApplyRequest;
import com.payhub.settlement.dto.ErrorOrderAuditRequest;
import com.payhub.settlement.dto.ErrorOrderVO;
import com.payhub.settlement.entity.ErrorOrder;

import java.util.List;
import java.util.Map;

public interface ErrorOrderService extends IService<ErrorOrder> {

    IPage<ErrorOrderVO> listPage(Long current, Long size, Map<String, Object> params);

    ErrorOrderVO getByErrorNo(String errorNo);

    ErrorOrderVO getById(Long id);

    ErrorOrderVO applyErrorOrder(ErrorOrderApplyRequest request, String applyUserId, String applyUserName);

    ErrorOrderVO auditErrorOrder(ErrorOrderAuditRequest request, String auditUserId, String auditUserName);

    ErrorOrderVO processSupplementOrder(Long errorOrderId, String handleUserId, String handleUserName);

    ErrorOrderVO processRefund(Long errorOrderId, String handleUserId, String handleUserName);

    ErrorOrderVO processAdjust(Long errorOrderId, String handleUserId, String handleUserName);

    ErrorOrderVO processIgnore(Long errorOrderId, String handleUserId, String handleUserName);

    List<ErrorOrderVO> listByReconcileDetailId(Long reconcileDetailId);
}
