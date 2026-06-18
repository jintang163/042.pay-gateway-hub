package com.payhub.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.settlement.dto.AgentWithdrawApplyRequest;
import com.payhub.settlement.dto.AgentWithdrawAuditRequest;
import com.payhub.settlement.dto.AgentWithdrawVO;
import com.payhub.settlement.entity.AgentWithdraw;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AgentWithdrawService extends IService<AgentWithdraw> {

    void applyWithdraw(AgentWithdrawApplyRequest request);

    void auditWithdraw(AgentWithdrawAuditRequest request);

    AgentWithdrawVO getWithdrawById(Long id);

    AgentWithdrawVO getWithdrawByWithdrawNo(String withdrawNo);

    IPage<AgentWithdrawVO> listPage(Long current, Long size, Map<String, Object> params);

    List<AgentWithdrawVO> listByMerchantNo(String merchantNo);

    BigDecimal getTotalWithdraw(String merchantNo, Integer withdrawStatus);

    BigDecimal getAvailableBalance(String merchantNo);

    void executeTransfer(Long id);

    void retryFailedWithdraw();
}
