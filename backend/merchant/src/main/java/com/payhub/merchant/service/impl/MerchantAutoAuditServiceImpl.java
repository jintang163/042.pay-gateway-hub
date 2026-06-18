package com.payhub.merchant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payhub.common.utils.JsonUtils;
import com.payhub.merchant.client.BusinessApiClient;
import com.payhub.merchant.dto.BusinessInfoDTO;
import com.payhub.merchant.entity.MerchantInfo;
import com.payhub.merchant.enums.AuditStepEnum;
import com.payhub.merchant.enums.RiskLevelEnum;
import com.payhub.merchant.mapper.MerchantInfoMapper;
import com.payhub.merchant.service.MerchantAutoAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
public class MerchantAutoAuditServiceImpl implements MerchantAutoAuditService {

    @Autowired
    private MerchantInfoMapper merchantInfoMapper;

    @Autowired
    private BusinessApiClient businessApiClient;

    private static final Random RANDOM = new Random();

    @Override
    @Async
    public void triggerAutoAudit(String merchantNo) {
        log.info("开始异步自动审核商户, merchantNo: {}", merchantNo);
        try {
            LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MerchantInfo::getMerchantNo, merchantNo).last("LIMIT 1");
            MerchantInfo merchant = merchantInfoMapper.selectOne(wrapper);
            if (merchant == null) {
                log.error("商户不存在, merchantNo: {}", merchantNo);
                return;
            }

            executeAutoAudit(merchant);

        } catch (Exception e) {
            log.error("商户自动审核异常, merchantNo: {}", merchantNo, e);
            try {
                LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(MerchantInfo::getMerchantNo, merchantNo).last("LIMIT 1");
                MerchantInfo merchant = merchantInfoMapper.selectOne(wrapper);
                if (merchant != null) {
                    merchant.setAuditStep(AuditStepEnum.MANUAL_AUDITING.getCode());
                    merchant.setAutoAuditRemark("自动审核异常，转人工处理: " + e.getMessage());
                    merchantInfoMapper.updateById(merchant);
                }
            } catch (Exception ex) {
                log.error("更新商户审核状态失败, merchantNo: {}", merchantNo, ex);
            }
        }
    }

    @Override
    public BusinessInfoDTO verifyBusinessLicense(MerchantInfo merchant) {
        log.info("开始核验营业执照, merchantNo: {}, licenseNo: {}", merchant.getMerchantNo(), merchant.getBusinessLicenseNo());

        updateAuditStep(merchant, AuditStepEnum.BUSINESS_VERIFYING);

        BusinessInfoDTO businessInfo = businessApiClient.queryBusinessInfo(merchant);
        BusinessApiClient.BusinessVerifyResult verifyResult = businessApiClient.verifyBusiness(merchant, businessInfo);

        merchant.setBusinessVerifyPassed(verifyResult.isPassed() ? 1 : 0);
        merchant.setBusinessVerifyResult(verifyResult.getDecisionJson());
        merchant.setBusinessVerifyTime(verifyResult.getVerifyTime());

        updateAuditStep(merchant, AuditStepEnum.BUSINESS_VERIFIED);

        log.info("营业执照核验完成, merchantNo: {}, passed: {}, score: {}",
                merchant.getMerchantNo(), verifyResult.isPassed(),
                businessInfo != null ? businessInfo.getMatchScore() : "null");

        return businessInfo;
    }

    @Override
    public Integer evaluateRisk(MerchantInfo merchant, BusinessInfoDTO businessInfo) {
        log.info("开始风险评估, merchantNo: {}", merchant.getMerchantNo());

        updateAuditStep(merchant, AuditStepEnum.RISK_EVALUATING);

        int riskScore = 0;

        if (merchant.getBusinessVerifyPassed() == null || merchant.getBusinessVerifyPassed() == 0) {
            riskScore += 40;
        } else if (businessInfo != null && businessInfo.getMatchScore() != null) {
            if (businessInfo.getMatchScore().compareTo(new BigDecimal("90")) < 0) {
                riskScore += 10;
            }
            if (businessInfo.getMatchScore().compareTo(new BigDecimal("70")) < 0) {
                riskScore += 15;
            }
        }

        String contactPhone = merchant.getContactPhone();
        if (StrUtil.isBlank(contactPhone)) {
            riskScore += 10;
        } else {
            String prefix = contactPhone.length() >= 3 ? contactPhone.substring(0, 3) : "";
            if ("170".equals(prefix) || "171".equals(prefix) || "165".equals(prefix)) {
                riskScore += 15;
            }
        }

        String merchantName = merchant.getMerchantName();
        if (StrUtil.isNotBlank(merchantName)) {
            if (merchantName.contains("金融") || merchantName.contains("贷款")
                    || merchantName.contains("投资") || merchantName.contains("担保")
                    || merchantName.contains("理财") || merchantName.contains("虚拟币")
                    || merchantName.contains("区块链") || merchantName.contains("外汇")) {
                riskScore += 30;
            }
            if (merchantName.contains("科技") || merchantName.contains("电子")
                    || merchantName.contains("贸易") || merchantName.contains("商务")) {
                riskScore -= 5;
            }
        }

        if (businessInfo != null && StrUtil.isNotBlank(businessInfo.getRegisteredCapital())) {
            String capital = businessInfo.getRegisteredCapital();
            if (capital.contains("1000") || capital.contains("5000") || capital.contains("10000")) {
                riskScore -= 5;
            }
            if (capital.contains("万元") && !capital.contains("100")) {
                riskScore -= 3;
            }
        }

        riskScore += RANDOM.nextInt(15);

        riskScore = Math.max(0, Math.min(100, riskScore));

        RiskLevelEnum riskLevel = RiskLevelEnum.getByScore(riskScore);
        merchant.setRiskScore(riskScore);
        merchant.setRiskLevel(riskLevel.getCode());

        updateAuditStep(merchant, AuditStepEnum.RISK_EVALUATED);

        log.info("风险评估完成, merchantNo: {}, riskScore: {}, riskLevel: {}",
                merchant.getMerchantNo(), riskScore, riskLevel.getName());

        return riskScore;
    }

    @Override
    public boolean executeAutoAudit(MerchantInfo merchant) {
        log.info("开始执行自动审核, merchantNo: {}", merchant.getMerchantNo());

        BusinessInfoDTO businessInfo = verifyBusinessLicense(merchant);

        Integer riskScore = evaluateRisk(merchant, businessInfo);

        RiskLevelEnum riskLevel = RiskLevelEnum.getByScore(riskScore);

        boolean autoPassed = false;
        String autoAuditRemark = "";

        if (RiskLevelEnum.LOW.equals(riskLevel) && (merchant.getBusinessVerifyPassed() != null && merchant.getBusinessVerifyPassed() == 1)) {
            autoPassed = true;
            autoAuditRemark = "自动审核通过: 低风险(" + riskScore + "分)，工商核验通过";
            merchant.setAuditStatus(1);
            merchant.setAuditRemark(autoAuditRemark);

            String md5Key = com.payhub.common.utils.SignUtil.generateMd5Key();
            com.payhub.common.utils.SignUtil.RsaKeyPair rsaKeyPair = com.payhub.common.utils.SignUtil.generateRsaKeyPair();
            com.payhub.common.utils.SignUtil.Sm2KeyPair sm2KeyPair = com.payhub.common.utils.SignUtil.generateSm2KeyPair();

            merchant.setApiKeyMd5(com.payhub.common.utils.Sm4Util.encrypt(md5Key));
            merchant.setApiKeyRsaPublic(rsaKeyPair.getPublicKey());
            merchant.setApiKeyRsaPrivate(com.payhub.common.utils.Sm4Util.encrypt(rsaKeyPair.getPrivateKey()));
            merchant.setApiKeySm2Public(sm2KeyPair.getPublicKey());
            merchant.setApiKeySm2Private(com.payhub.common.utils.Sm4Util.encrypt(sm2KeyPair.getPrivateKey()));

            log.info("低风险商户自动审核通过, 已生成密钥, merchantNo: {}", merchant.getMerchantNo());
        } else if (RiskLevelEnum.HIGH.equals(riskLevel)) {
            autoPassed = false;
            autoAuditRemark = "高风险(" + riskScore + "分)，转人工审核";
            merchant.setAuditStep(AuditStepEnum.MANUAL_AUDITING.getCode());
            log.info("高风险商户转人工审核, merchantNo: {}, riskScore: {}", merchant.getMerchantNo(), riskScore);
        } else {
            autoPassed = false;
            if (merchant.getBusinessVerifyPassed() == null || merchant.getBusinessVerifyPassed() == 0) {
                autoAuditRemark = "工商核验未通过，转人工审核";
            } else {
                autoAuditRemark = "中风险(" + riskScore + "分)，转人工复核";
            }
            merchant.setAuditStep(AuditStepEnum.MANUAL_AUDITING.getCode());
            log.info("中风险商户转人工复核, merchantNo: {}, riskScore: {}", merchant.getMerchantNo(), riskScore);
        }

        merchant.setAutoAuditPassed(autoPassed ? 1 : 0);
        merchant.setAutoAuditRemark(autoAuditRemark);
        merchant.setAutoAuditTime(LocalDateTime.now());

        if (AuditStepEnum.MANUAL_AUDITING.getCode().equals(merchant.getAuditStep())) {
            updateAuditStep(merchant, AuditStepEnum.MANUAL_AUDITING);
        } else {
            updateAuditStep(merchant, AuditStepEnum.AUTO_AUDIT_DONE);
        }

        merchantInfoMapper.updateById(merchant);

        log.info("自动审核执行完成, merchantNo: {}, autoPassed: {}, remark: {}",
                merchant.getMerchantNo(), autoPassed, autoAuditRemark);

        return autoPassed;
    }

    private void updateAuditStep(MerchantInfo merchant, AuditStepEnum step) {
        merchant.setAuditStep(step.getCode());
        merchantInfoMapper.updateById(merchant);
        log.debug("更新商户审核步骤, merchantNo: {}, step: {} - {}",
                merchant.getMerchantNo(), step.getCode(), step.getName());
    }
}
