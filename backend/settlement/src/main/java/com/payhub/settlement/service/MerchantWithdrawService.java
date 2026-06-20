package com.payhub.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.settlement.dto.MerchantBalanceVO;
import com.payhub.settlement.dto.MerchantWithdrawApplyRequest;
import com.payhub.settlement.dto.MerchantWithdrawAuditRequest;
import com.payhub.settlement.dto.MerchantWithdrawVO;
import com.payhub.settlement.entity.MerchantWithdraw;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface MerchantWithdrawService extends IService<MerchantWithdraw> {

    void applyWithdraw(MerchantWithdrawApplyRequest request);

    void auditWithdraw(MerchantWithdrawAuditRequest request);

    MerchantWithdrawVO getWithdrawById(Long id);

    MerchantWithdrawVO getWithdrawByWithdrawNo(String withdrawNo);

    IPage<MerchantWithdrawVO> listPage(Long current, Long size, Map<String, Object> params);

    List<MerchantWithdrawVO> listByMerchantNo(String merchantNo);

    BigDecimal getTotalWithdraw(String merchantNo, Integer withdrawStatus);

    BigDecimal getAvailableBalance(String merchantNo);

    MerchantBalanceVO getMerchantBalance(String merchantNo);

    void executeTransfer(Long id);

    void retryFailedWithdraw();

    void processT1Batch(int batchSize);

    void updateNextRetryTime(Long id);

    BigDecimal calculateFee(BigDecimal amount, Integer withdrawType);

    com.payhub.merchant.dto.FeePromotionCalcResult calculateFeeWithPromotion(String merchantNo, BigDecimal amount, Integer withdrawType);
}
