package com.payhub.merchant.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.*;
import com.payhub.merchant.dto.*;
import com.payhub.merchant.entity.MerchantInfo;
import com.payhub.merchant.mapper.MerchantInfoMapper;
import com.payhub.merchant.service.MerchantInfoService;
import com.payhub.merchant.enums.AuditStepEnum;
import com.payhub.merchant.enums.RiskLevelEnum;
import com.payhub.merchant.service.MerchantAutoAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MerchantInfoServiceImpl extends ServiceImpl<MerchantInfoMapper, MerchantInfo> implements MerchantInfoService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private MerchantAutoAuditService merchantAutoAuditService;

    @Autowired(required = false)
    private com.payhub.merchant.service.FeePromotionService feePromotionService;

    private static final String SMS_CODE_CACHE_KEY = "payhub:sms:reset_api_key:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantApplyResult apply(MerchantApplyRequest request) {
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getBusinessLicenseNo, request.getBusinessLicenseNo());
        Long count = this.count(wrapper);
        if (count > 0) {
            throw new BusinessException(ResultCode.MERCHANT_APPLY_EXIST);
        }

        String merchantNo = SnowflakeIdUtil.generateMerchantNo();
        AuditStepEnum step = AuditStepEnum.DATA_SUBMITTED;

        MerchantInfo merchant = new MerchantInfo();
        merchant.setMerchantNo(merchantNo);
        merchant.setMerchantName(request.getMerchantName());
        merchant.setBusinessLicenseNo(request.getBusinessLicenseNo());
        merchant.setLegalPersonName(request.getLegalPersonName());
        merchant.setLegalPersonIdNo(Sm4Util.encrypt(request.getLegalPersonIdNo()));
        merchant.setContactPhone(request.getContactPhone());
        merchant.setContactEmail(request.getContactEmail());
        merchant.setSettlementBankName(request.getSettlementBankName());
        merchant.setSettlementBankAccount(Sm4Util.encrypt(request.getSettlementBankAccount()));
        merchant.setSettlementAccountName(request.getSettlementAccountName());
        merchant.setAuditStatus(0);
        merchant.setStatus(1);
        merchant.setAuditStep(step.getCode());

        this.save(merchant);

        log.info("商户入驻申请提交成功: merchantNo={}, merchantName={}", merchantNo, request.getMerchantName());

        final String finalMerchantNo = merchantNo;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.debug("事务已提交，触发异步审核, merchantNo={}", finalMerchantNo);
                    merchantAutoAuditService.triggerAutoAudit(finalMerchantNo);
                }
            });
        } else {
            merchantAutoAuditService.triggerAutoAudit(merchantNo);
        }

        return MerchantApplyResult.builder()
                .merchantNo(merchantNo)
                .merchantName(request.getMerchantName())
                .auditStep(step.getCode())
                .auditStepName(step.getName())
                .auditStatus(0)
                .auditStatusDesc("待审核")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(MerchantAuditRequest request) {
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getMerchantNo, request.getMerchantNo())
                .last("LIMIT 1");
        MerchantInfo merchant = this.getOne(wrapper);
        if (merchant == null) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_EXIST);
        }

        merchant.setAuditStatus(request.getAuditStatus());
        merchant.setAuditRemark(request.getAuditRemark());
        merchant.setManualAuditUser(request.getAuditUserName());
        merchant.setManualAuditTime(LocalDateTime.now());
        merchant.setAuditStep(AuditStepEnum.MANUAL_AUDITING.getCode());

        if (request.getAuditStatus() == 1) {
            String md5Key = SignUtil.generateMd5Key();
            SignUtil.RsaKeyPair rsaKeyPair = SignUtil.generateRsaKeyPair();

            merchant.setApiKeyMd5(Sm4Util.encrypt(md5Key));
            merchant.setApiKeyRsaPublic(rsaKeyPair.getPublicKey());
            merchant.setApiKeyRsaPrivate(Sm4Util.encrypt(rsaKeyPair.getPrivateKey()));
        }

        this.updateById(merchant);

        if (request.getAuditStatus() == 1 && feePromotionService != null) {
            try {
                feePromotionService.bindNewMerchantPromotion(
                        request.getMerchantNo(),
                        merchant.getMerchantName(),
                        merchant.getIndustryCode());
                log.info("商户审核通过，自动绑定新商户费率优惠活动: merchantNo={}", request.getMerchantNo());
            } catch (Exception e) {
                log.warn("绑定新商户费率优惠活动失败: merchantNo={}", request.getMerchantNo(), e);
            }
        }

        log.info("商户人工审核完成: merchantNo={}, auditStatus={}, auditUser={}",
                request.getMerchantNo(), request.getAuditStatus(), request.getAuditUserName());
    }

    @Override
    public MerchantVO getByMerchantNo(String merchantNo) {
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getMerchantNo, merchantNo)
                .last("LIMIT 1");
        MerchantInfo merchant = this.getOne(wrapper);
        if (merchant == null) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_EXIST);
        }
        return convertToVO(merchant);
    }

    @Override
    public IPage<MerchantVO> listPage(Long current, Long size, Map<String, Object> params) {
        Page<MerchantInfo> page = new Page<>(current, size);
        IPage<MerchantInfo> infoPage = this.baseMapper.selectPageList(page, params);

        return infoPage.convert(this::convertToVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiKeyVO resetApiKey(ApiKeyResetRequest request) {
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getMerchantNo, request.getMerchantNo())
                .last("LIMIT 1");
        MerchantInfo merchant = this.getOne(wrapper);
        if (merchant == null) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_EXIST);
        }

        String cacheKey = SMS_CODE_CACHE_KEY + request.getMerchantNo();
        String cachedCode = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedCode == null || !cachedCode.equals(request.getSmsCode())) {
            throw new BusinessException(ResultCode.SMS_CODE_ERROR);
        }
        stringRedisTemplate.delete(cacheKey);

        String md5Key = SignUtil.generateMd5Key();
        SignUtil.RsaKeyPair rsaKeyPair = SignUtil.generateRsaKeyPair();
        SignUtil.Sm2KeyPair sm2KeyPair = SignUtil.generateSm2KeyPair();

        merchant.setApiKeyMd5(Sm4Util.encrypt(md5Key));
        merchant.setApiKeyRsaPublic(rsaKeyPair.getPublicKey());
        merchant.setApiKeyRsaPrivate(Sm4Util.encrypt(rsaKeyPair.getPrivateKey()));

        this.updateById(merchant);

        ApiKeyVO vo = new ApiKeyVO();
        vo.setMerchantNo(request.getMerchantNo());
        vo.setMd5Key(md5Key);
        vo.setRsaPublicKey(rsaKeyPair.getPublicKey());
        vo.setRsaPrivateKey(rsaKeyPair.getPrivateKey());
        vo.setSm2PublicKey(sm2KeyPair.getPublicKey());
        vo.setSm2PrivateKey(sm2KeyPair.getPrivateKey());

        log.info("商户密钥重置成功: merchantNo={}, signType={}", request.getMerchantNo(), request.getSignType());
        return vo;
    }

    @Override
    public boolean testCallback(String merchantNo, String url) {
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getMerchantNo, merchantNo)
                .last("LIMIT 1");
        MerchantInfo merchant = this.getOne(wrapper);
        if (merchant == null) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_EXIST);
        }
        if (merchant.getAuditStatus() != 1) {
            throw new BusinessException(ResultCode.MERCHANT_AUDIT_PENDING);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("merchantNo", merchantNo);
        params.put("testId", IdUtil.fastSimpleUUID());
        params.put("timestamp", System.currentTimeMillis());
        params.put("amount", 100);
        params.put("orderNo", "TEST" + System.currentTimeMillis());

        String signType = "MD5";
        String md5Key = Sm4Util.decrypt(merchant.getApiKeyMd5());
        String rsaPublicKey = merchant.getApiKeyRsaPublic();
        String rsaPrivateKey = Sm4Util.decrypt(merchant.getApiKeyRsaPrivate());

        String sign = SignUtil.sign(params, signType, md5Key, rsaPrivateKey, null);
        params.put("sign", sign);
        params.put("signType", signType);

        log.info("回调测试请求: url={}, params={}", url, params);
        String respStr = HttpUtil.postJson(url, params);
        log.info("回调测试响应: url={}, resp={}", url, respStr);

        if (StrUtil.isBlank(respStr)) {
            throw new BusinessException(ResultCode.CALLBACK_TEST_FAIL, "回调请求无响应");
        }

        try {
            JSONObject resp = JSON.parseObject(respStr);
            String respSign = resp.getString("sign");
            String respSignType = resp.getString("signType");
            if (StrUtil.isBlank(respSign)) {
                throw new BusinessException(ResultCode.CALLBACK_TEST_FAIL, "回调响应缺少签名");
            }

            Map<String, Object> respParams = new HashMap<>();
            for (String key : resp.keySet()) {
                if (!"sign".equals(key) && !"signType".equals(key)) {
                    respParams.put(key, resp.get(key));
                }
            }

            String currentSignType = StrUtil.isNotBlank(respSignType) ? respSignType : signType;
            boolean verifyResult = SignUtil.verify(respParams, currentSignType, respSign, md5Key, rsaPublicKey, null);

            if (!verifyResult) {
                throw new BusinessException(ResultCode.CALLBACK_TEST_FAIL, "回调响应签名验证失败");
            }

            log.info("回调测试成功: merchantNo={}, url={}", merchantNo, url);
            return true;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("回调测试异常", e);
            throw new BusinessException(ResultCode.CALLBACK_TEST_FAIL, "回调响应解析失败: " + e.getMessage());
        }
    }

    private MerchantVO convertToVO(MerchantInfo merchant) {
        MerchantVO vo = BeanUtil.copyProperties(merchant, MerchantVO.class);
        vo.setSettlementBankAccount(maskBankAccount(merchant.getSettlementBankAccount()));
        vo.setAuditStatusDesc(getAuditStatusDesc(merchant.getAuditStatus()));
        vo.setStatusDesc(merchant.getStatus() == 1 ? "启用" : "禁用");
        if (merchant.getAuditStep() != null) {
            AuditStepEnum stepEnum = AuditStepEnum.getByCode(merchant.getAuditStep());
            if (stepEnum != null) {
                vo.setAuditStepName(stepEnum.getName());
            }
        }
        if (StrUtil.isNotBlank(merchant.getRiskLevel())) {
            RiskLevelEnum riskLevelEnum = RiskLevelEnum.getByCode(merchant.getRiskLevel());
            if (riskLevelEnum != null) {
                vo.setRiskLevelDesc(riskLevelEnum.getName());
            }
        }
        return vo;
    }

    private String maskBankAccount(String account) {
        if (StrUtil.isBlank(account)) {
            return "";
        }
        String decrypted = Sm4Util.decrypt(account);
        if (decrypted.length() <= 8) {
            return decrypted;
        }
        return decrypted.substring(0, 4) + "****" + decrypted.substring(decrypted.length() - 4);
    }

    private String getAuditStatusDesc(Integer auditStatus) {
        if (auditStatus == null) {
            return "";
        }
        switch (auditStatus) {
            case 0:
                return "待审核";
            case 1:
                return "审核通过";
            case 2:
                return "审核拒绝";
            default:
                return "";
        }
    }

    @Override
    public AuditProgressVO getAuditProgress(String merchantNo) {
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getMerchantNo, merchantNo).last("LIMIT 1");
        MerchantInfo merchant = this.getOne(wrapper);
        if (merchant == null) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_EXIST);
        }

        AuditProgressVO vo = new AuditProgressVO();
        vo.setMerchantNo(merchant.getMerchantNo());
        vo.setMerchantName(merchant.getMerchantName());
        vo.setAuditStatus(merchant.getAuditStatus());
        vo.setAuditStatusDesc(getAuditStatusDesc(merchant.getAuditStatus()));
        vo.setAuditRemark(merchant.getAuditRemark());

        if (merchant.getAuditStep() != null) {
            AuditStepEnum stepEnum = AuditStepEnum.getByCode(merchant.getAuditStep());
            if (stepEnum != null) {
                vo.setAuditStep(stepEnum.getCode());
                vo.setAuditStepName(stepEnum.getName());
                vo.setAuditStepDescription(stepEnum.getDescription());
            }
        }

        vo.setRiskLevel(merchant.getRiskLevel());
        if (StrUtil.isNotBlank(merchant.getRiskLevel())) {
            RiskLevelEnum riskLevelEnum = RiskLevelEnum.getByCode(merchant.getRiskLevel());
            if (riskLevelEnum != null) {
                vo.setRiskLevelDesc(riskLevelEnum.getName());
            }
        }
        vo.setRiskScore(merchant.getRiskScore());
        vo.setBusinessVerifyPassed(merchant.getBusinessVerifyPassed());
        vo.setBusinessVerifyResult(merchant.getBusinessVerifyResult());
        vo.setBusinessVerifyTime(merchant.getBusinessVerifyTime());
        vo.setAutoAuditPassed(merchant.getAutoAuditPassed());
        vo.setAutoAuditRemark(merchant.getAutoAuditRemark());
        vo.setAutoAuditTime(merchant.getAutoAuditTime());
        vo.setManualAuditUser(merchant.getManualAuditUser());
        vo.setManualAuditTime(merchant.getManualAuditTime());

        if (StrUtil.isNotBlank(merchant.getBusinessVerifyResult())) {
            vo.setVerifyDetail(parseVerifyDetail(merchant.getBusinessVerifyResult()));
        }

        List<AuditProgressVO.AuditStepItem> stepItems = buildStepItems(merchant);
        vo.setSteps(stepItems);

        return vo;
    }

    private List<AuditProgressVO.AuditStepItem> buildStepItems(MerchantInfo merchant) {
        List<AuditProgressVO.AuditStepItem> items = new ArrayList<>();

        items.add(createStepItem(1, merchant, null, null));
        items.add(createStepItem(2, merchant, AuditStepEnum.BUSINESS_VERIFYING, merchant.getBusinessVerifyTime()));
        items.add(createStepItem(3, merchant, AuditStepEnum.BUSINESS_VERIFIED, merchant.getBusinessVerifyTime()));
        items.add(createStepItem(4, merchant, AuditStepEnum.RISK_EVALUATING, merchant.getAutoAuditTime()));
        items.add(createStepItem(5, merchant, AuditStepEnum.RISK_EVALUATED, merchant.getAutoAuditTime()));
        items.add(createStepItem(6, merchant, AuditStepEnum.AUTO_AUDIT_DONE, merchant.getAutoAuditTime()));
        items.add(createStepItem(7, merchant, AuditStepEnum.MANUAL_AUDITING, merchant.getManualAuditTime()));

        for (AuditProgressVO.AuditStepItem item : items) {
            if (merchant.getAuditStep() != null && merchant.getAuditStep() > item.getStep()) {
                item.setStatus("done");
            } else if (merchant.getAuditStep() != null && merchant.getAuditStep().equals(item.getStep())) {
                item.setStatus("active");
                if (item.getStep() == 3 && merchant.getBusinessVerifyPassed() != null) {
                    item.setRemark(merchant.getBusinessVerifyPassed() == 1 ? "核验通过" : "核验未通过");
                }
                if (item.getStep() == 5 && merchant.getRiskScore() != null) {
                    item.setRemark("风险评分: " + merchant.getRiskScore() + "分");
                }
                if (item.getStep() == 6 && merchant.getAutoAuditPassed() != null) {
                    item.setRemark(merchant.getAutoAuditPassed() == 1 ? "自动通过" : "转人工");
                }
            } else {
                item.setStatus("pending");
            }
        }

        return items;
    }

    private AuditProgressVO.AuditStepItem createStepItem(Integer step, MerchantInfo merchant,
                                                         AuditStepEnum stepEnum, LocalDateTime time) {
        AuditProgressVO.AuditStepItem item = new AuditProgressVO.AuditStepItem();
        AuditStepEnum e = stepEnum != null ? stepEnum : AuditStepEnum.getByCode(step);
        item.setStep(step);
        if (e != null) {
            item.setName(e.getName());
            item.setDescription(e.getDescription());
        }
        item.setTime(time);
        return item;
    }

    @SuppressWarnings("unchecked")
    private AuditProgressVO.VerifyDetail parseVerifyDetail(String decisionJson) {
        try {
            Map<String, Object> map = JsonUtils.parseObject(decisionJson, Map.class);
            if (map == null || map.isEmpty()) {
                return null;
            }
            AuditProgressVO.VerifyDetail detail = new AuditProgressVO.VerifyDetail();
            detail.setVerifyId((String) map.get("verifyId"));
            detail.setVerifyVendor((String) map.get("verifyVendor"));
            detail.setVerifySource((String) map.get("verifySource"));
            detail.setVerifyRequestId((String) map.get("verifyRequestId"));
            Object fallback = map.get("fallbackUsed");
            detail.setFallbackUsed(Boolean.TRUE.equals(fallback));
            Object score = map.get("matchOverallScore");
            if (score != null) {
                detail.setMatchOverallScore(new BigDecimal(score.toString()));
            }
            Object reasons = map.get("decisionReasons");
            if (reasons instanceof List) {
                detail.setDecisionReasons((List<String>) reasons);
            }
            detail.setFailReason((String) map.get("failReason"));
            detail.setRawRequest((String) map.get("rawRequest"));
            detail.setRawResponse((String) map.get("rawResponse"));
            detail.setVerifiedBy((String) map.get("verifiedBy"));
            Object time = map.get("verifyTime");
            if (time != null) {
                detail.setVerifyTime(LocalDateTime.parse(time.toString()));
            }
            return detail;
        } catch (Exception e) {
            log.warn("解析工商核验决策详情失败", e);
            return null;
        }
    }

    @Override
    public Map<String, Integer> getManualAuditStats() {
        Map<String, Integer> stats = new LinkedHashMap<>();

        LambdaQueryWrapper<MerchantInfo> pendingAll = new LambdaQueryWrapper<>();
        pendingAll.eq(MerchantInfo::getAuditStatus, 0);
        stats.put("totalPending", Math.toIntExact(this.count(pendingAll)));

        LambdaQueryWrapper<MerchantInfo> highRisk = new LambdaQueryWrapper<>();
        highRisk.eq(MerchantInfo::getAuditStatus, 0).eq(MerchantInfo::getRiskLevel, "HIGH");
        stats.put("highRisk", Math.toIntExact(this.count(highRisk)));

        LambdaQueryWrapper<MerchantInfo> mediumRisk = new LambdaQueryWrapper<>();
        mediumRisk.eq(MerchantInfo::getAuditStatus, 0).eq(MerchantInfo::getRiskLevel, "MEDIUM");
        stats.put("mediumRisk", Math.toIntExact(this.count(mediumRisk)));

        LambdaQueryWrapper<MerchantInfo> lowRisk = new LambdaQueryWrapper<>();
        lowRisk.eq(MerchantInfo::getAuditStatus, 0).eq(MerchantInfo::getRiskLevel, "LOW");
        stats.put("lowRisk", Math.toIntExact(this.count(lowRisk)));

        LambdaQueryWrapper<MerchantInfo> needManual = new LambdaQueryWrapper<>();
        needManual.eq(MerchantInfo::getAuditStatus, 0).eq(MerchantInfo::getAuditStep, AuditStepEnum.MANUAL_AUDITING.getCode());
        stats.put("needManual", Math.toIntExact(this.count(needManual)));

        LambdaQueryWrapper<MerchantInfo> businessFail = new LambdaQueryWrapper<>();
        businessFail.eq(MerchantInfo::getAuditStatus, 0).eq(MerchantInfo::getBusinessVerifyPassed, 0);
        stats.put("businessFail", Math.toIntExact(this.count(businessFail)));

        return stats;
    }
}
