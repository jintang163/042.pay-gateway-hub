package com.payhub.merchant.client;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.payhub.common.utils.JsonUtils;
import com.payhub.merchant.dto.BusinessInfoDTO;
import com.payhub.merchant.entity.MerchantInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Component
public class BusinessApiClient {

    private static final String MOCK_SOURCE = "国家企业信用信息公示系统(模拟)";

    public BusinessInfoDTO queryBusinessInfo(MerchantInfo merchant) {
        log.info("开始调用工商信息API, 商户: {}, 营业执照: {}", merchant.getMerchantNo(), merchant.getBusinessLicenseNo());

        try {
            Thread.sleep(800 + new Random().nextInt(1200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        BusinessInfoDTO info = new BusinessInfoDTO();
        info.setBusinessLicenseNo(merchant.getBusinessLicenseNo());
        info.setLegalPersonName(merchant.getLegalPersonName());
        info.setVerifyTime(LocalDateTime.now());
        info.setVerifySource(MOCK_SOURCE);

        String licenseNo = merchant.getBusinessLicenseNo();
        if (StrUtil.isBlank(licenseNo) || licenseNo.length() < 15) {
            info.setMerchantName(null);
            info.setMatchScore(BigDecimal.ZERO);
            info.setBusinessStatus("吊销");
            log.warn("工商核验失败: 营业执照号格式无效, 商户: {}", merchant.getMerchantNo());
            return info;
        }

        if (licenseNo.startsWith("X")) {
            info.setMerchantName("已注销企业-" + merchant.getMerchantName());
            info.setMatchScore(new BigDecimal("15.5"));
            info.setBusinessStatus("注销");
            log.warn("工商核验失败: 企业已注销, 商户: {}", merchant.getMerchantNo());
            return info;
        }

        info.setMerchantName(merchant.getMerchantName());
        info.setRegisteredCapital("100万元人民币");
        info.setEstablishmentDate(LocalDate.of(2018 + new Random().nextInt(5), 1 + new Random().nextInt(12), 1 + new Random().nextInt(28)).toString());
        info.setBusinessScope("计算机软硬件开发、技术服务、电子产品销售、国内贸易");
        info.setRegisteredAddress("上海市浦东新区张江高科技园区博云路" + (100 + new Random().nextInt(900)) + "号");
        info.setEnterpriseType("有限责任公司(自然人投资或控股)");
        info.setBusinessStatus("存续");

        if (licenseNo.startsWith("T")) {
            info.setLegalPersonName("不匹配-" + merchant.getLegalPersonName());
            info.setMatchScore(new BigDecimal("45.0"));
        } else {
            info.setMatchScore(new BigDecimal(90 + new Random().nextInt(10)).add(new BigDecimal(new Random().nextDouble())).setScale(1, java.math.RoundingMode.HALF_UP));
        }

        log.info("工商信息API调用完成, 商户: {}, 匹配度: {}, 状态: {}", merchant.getMerchantNo(), info.getMatchScore(), info.getBusinessStatus());
        return info;
    }

    public BusinessVerifyResult verifyBusiness(MerchantInfo merchant, BusinessInfoDTO businessInfo) {
        BusinessVerifyResult result = new BusinessVerifyResult();
        result.setVerifyId(IdUtil.fastSimpleUUID());
        result.setVerifyTime(LocalDateTime.now());
        result.setBusinessInfo(businessInfo);

        if (businessInfo == null || businessInfo.getMatchScore() == null) {
            result.setPassed(false);
            result.setFailReason("工商信息查询失败");
            return result;
        }

        StringBuilder failReasons = new StringBuilder();
        boolean passed = true;

        if (businessInfo.getMatchScore().compareTo(new BigDecimal("60")) < 0) {
            passed = false;
            failReasons.append("工商信息匹配度不足60分;");
        }

        if (!"存续".equals(businessInfo.getBusinessStatus()) && !"开业".equals(businessInfo.getBusinessStatus())) {
            passed = false;
            failReasons.append("企业状态异常: ").append(businessInfo.getBusinessStatus()).append(";");
        }

        if (StrUtil.isNotBlank(businessInfo.getLegalPersonName())
                && !businessInfo.getLegalPersonName().equals(merchant.getLegalPersonName())) {
            passed = false;
            failReasons.append("法人姓名不匹配;");
        }

        if (StrUtil.isNotBlank(businessInfo.getMerchantName())
                && !businessInfo.getMerchantName().equals(merchant.getMerchantName())) {
            if (businessInfo.getMatchScore().compareTo(new BigDecimal("80")) < 0) {
                passed = false;
                failReasons.append("企业名称不匹配;");
            }
        }

        result.setPassed(passed);
        result.setFailReason(failReasons.length() > 0 ? failReasons.toString() : null);
        result.setRawResponse(JsonUtils.toJsonString(businessInfo));

        return result;
    }

    @lombok.Data
    public static class BusinessVerifyResult {
        private String verifyId;
        private boolean passed;
        private String failReason;
        private BusinessInfoDTO businessInfo;
        private String rawResponse;
        private LocalDateTime verifyTime;
    }
}
