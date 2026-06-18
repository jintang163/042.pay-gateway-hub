package com.payhub.merchant.client;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.payhub.common.utils.JsonUtils;
import com.payhub.merchant.config.BusinessApiProperties;
import com.payhub.merchant.dto.BusinessInfoDTO;
import com.payhub.merchant.entity.MerchantInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
public class BusinessApiClient {

    private static final String VENDOR_NAME = "国家企业信用信息公示系统";
    private static final String SANDBOX_VENDOR = VENDOR_NAME + "(沙箱模拟)";
    private static final String VERIFIED_BY_AUTO = "SYSTEM_AUTO";

    @Autowired
    private BusinessApiProperties properties;

    public BusinessInfoDTO queryBusinessInfo(MerchantInfo merchant) {
        if (properties.getSandbox()) {
            return queryBusinessInfoSandbox(merchant);
        }
        return queryBusinessInfoReal(merchant);
    }

    private BusinessInfoDTO queryBusinessInfoReal(MerchantInfo merchant) {
        String requestId = IdUtil.fastSimpleUUID();
        log.info("[工商API-真实] 调用开始, requestId={}, merchantNo={}, license={}",
                requestId, merchant.getMerchantNo(), merchant.getBusinessLicenseNo());

        long startTime = System.currentTimeMillis();
        Map<String, Object> requestPayload = buildRequestPayload(merchant, requestId);
        BusinessInfoDTO info = new BusinessInfoDTO();
        info.setBusinessLicenseNo(merchant.getBusinessLicenseNo());
        info.setVerifyRequestId(requestId);
        info.setVerifyVendor(VENDOR_NAME);
        info.setVerifyTime(LocalDateTime.now());

        int retryTimes = Math.max(0, Optional.ofNullable(properties.getRetryTimes()).orElse(0));
        int attempts = 0;
        while (attempts <= retryTimes) {
            attempts++;
            try {
                HttpResponse response = HttpRequest.post(properties.getUrl() + "/query")
                        .header("Content-Type", "application/json")
                        .header("X-App-Key", properties.getAppKey())
                        .header("X-Signature", signPayload(requestPayload))
                        .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
                        .body(JsonUtils.toJsonString(requestPayload))
                        .timeout(properties.getTimeoutMs())
                        .execute();

                if (response.getStatus() == 200) {
                    String body = response.body();
                    info.setVerifyRawRequest(JsonUtils.toJsonString(requestPayload));
                    info.setVerifyRawResponse(body);
                    info.setVerifySource(VENDOR_NAME + "(真实)");

                    Map<String, Object> respMap = parseResponse(body);
                    if (respMap != null && Boolean.TRUE.equals(respMap.get("success"))) {
                        Map<String, Object> data = (Map<String, Object>) respMap.get("data");
                        fillBusinessInfoFromResponse(info, data);
                        long cost = System.currentTimeMillis() - startTime;
                        log.info("[工商API-真实] 调用成功, requestId={}, cost={}ms, matchScore={}", requestId, cost, info.getMatchScore());
                        return info;
                    } else {
                        String msg = respMap != null ? (String) respMap.get("message") : "响应解析失败";
                        log.warn("[工商API-真实] 调用失败, requestId={}, attempt={}/{}, message={}", requestId, attempts, retryTimes + 1, msg);
                    }
                } else {
                    log.warn("[工商API-真实] HTTP状态异常, requestId={}, attempt={}/{}, status={}", requestId, attempts, retryTimes + 1, response.getStatus());
                }
            } catch (Exception e) {
                log.error("[工商API-真实] 调用异常, requestId={}, attempt={}/{}", requestId, attempts, retryTimes + 1, e);
            }
            if (attempts <= retryTimes) {
                try {
                    Thread.sleep(500L * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        long cost = System.currentTimeMillis() - startTime;
        log.warn("[工商API-真实] 全部重试失败, requestId={}, cost={}ms, 降级为沙箱模式", requestId, cost);
        BusinessInfoDTO fallback = queryBusinessInfoSandbox(merchant);
        fallback.setVerifyRequestId(requestId);
        fallback.setVerifyVendor(VENDOR_NAME);
        fallback.setVerifySource(VENDOR_NAME + "(降级沙箱)");
        fallback.setFallbackUsed(true);
        return fallback;
    }

    private Map<String, Object> buildRequestPayload(MerchantInfo merchant, String requestId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", requestId);
        payload.put("businessLicenseNo", merchant.getBusinessLicenseNo());
        payload.put("merchantName", merchant.getMerchantName());
        payload.put("legalPersonName", merchant.getLegalPersonName());
        payload.put("contactPhone", merchant.getContactPhone());
        return payload;
    }

    private String signPayload(Map<String, Object> payload) {
        try {
            String content = JsonUtils.toJsonString(payload) + "|" + properties.getAppSecret();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("签名计算失败", e);
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(String body) {
        try {
            return JsonUtils.parseObject(body, Map.class);
        } catch (Exception e) {
            log.warn("工商API响应JSON解析失败, body={}", StrUtil.maxLength(body, 300), e);
            return null;
        }
    }

    private void fillBusinessInfoFromResponse(BusinessInfoDTO info, Map<String, Object> data) {
        if (data == null) {
            info.setBusinessStatus("未知");
            info.setMatchScore(BigDecimal.ZERO);
            return;
        }
        info.setMerchantName((String) data.get("merchantName"));
        info.setLegalPersonName((String) data.get("legalPersonName"));
        info.setRegisteredCapital((String) data.get("registeredCapital"));
        info.setEstablishmentDate((String) data.get("establishmentDate"));
        info.setBusinessScope((String) data.get("businessScope"));
        info.setRegisteredAddress((String) data.get("registeredAddress"));
        info.setEnterpriseType((String) data.get("enterpriseType"));
        info.setBusinessStatus((String) data.get("businessStatus"));
        Object score = data.get("matchScore");
        if (score instanceof BigDecimal) {
            info.setMatchScore((BigDecimal) score);
        } else if (score instanceof Number) {
            info.setMatchScore(new BigDecimal(score.toString()));
        } else if (score instanceof String && StrUtil.isNotBlank((String) score)) {
            info.setMatchScore(new BigDecimal((String) score));
        } else {
            info.setMatchScore(BigDecimal.ZERO);
        }
        Map<String, Object> matchScores = (Map<String, Object>) data.get("matchScores");
        if (matchScores != null) {
            info.setMatchScores(JsonUtils.toJsonString(matchScores));
        }
    }

    private BusinessInfoDTO queryBusinessInfoSandbox(MerchantInfo merchant) {
        String requestId = IdUtil.fastSimpleUUID();
        log.info("[工商API-沙箱] 调用开始, requestId={}, merchantNo={}", requestId, merchant.getMerchantNo());
        long startTime = System.currentTimeMillis();
        Random random = new Random();

        try {
            Thread.sleep(800 + random.nextInt(1200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        BusinessInfoDTO info = new BusinessInfoDTO();
        info.setBusinessLicenseNo(merchant.getBusinessLicenseNo());
        info.setVerifyRequestId(requestId);
        info.setVerifyVendor(SANDBOX_VENDOR);
        info.setVerifySource(SANDBOX_VENDOR);
        info.setVerifyTime(LocalDateTime.now());
        info.setLegalPersonName(merchant.getLegalPersonName());

        Map<String, Object> requestPayload = buildRequestPayload(merchant, requestId);
        info.setVerifyRawRequest(JsonUtils.toJsonString(requestPayload));

        String licenseNo = merchant.getBusinessLicenseNo();
        if (StrUtil.isBlank(licenseNo) || licenseNo.length() < 15) {
            info.setMerchantName(null);
            info.setMatchScore(BigDecimal.ZERO);
            info.setBusinessStatus("吊销");
            Map<String, Object> matchScores = buildMatchScores(0, 0, 0, 0);
            info.setMatchScores(JsonUtils.toJsonString(matchScores));
            info.setVerifyRawResponse(JsonUtils.toJsonString(buildSandboxResp(false, "营业执照号格式无效", info, matchScores)));
            log.warn("[工商API-沙箱] 营业执照号无效, merchantNo={}", merchant.getMerchantNo());
            return info;
        }

        if (licenseNo.startsWith("X")) {
            info.setMerchantName("已注销企业-" + merchant.getMerchantName());
            info.setMatchScore(new BigDecimal("15.5"));
            info.setBusinessStatus("注销");
            Map<String, Object> matchScores = buildMatchScores(15, 0, 20, 10);
            info.setMatchScores(JsonUtils.toJsonString(matchScores));
            info.setVerifyRawResponse(JsonUtils.toJsonString(buildSandboxResp(false, "企业已注销", info, matchScores)));
            log.warn("[工商API-沙箱] 企业已注销, merchantNo={}", merchant.getMerchantNo());
            return info;
        }

        info.setMerchantName(merchant.getMerchantName());
        info.setRegisteredCapital("100万元人民币");
        info.setEstablishmentDate(LocalDate.of(2018 + random.nextInt(5), 1 + random.nextInt(12), 1 + random.nextInt(28)).toString());
        info.setBusinessScope("计算机软硬件开发、技术服务、电子产品销售、国内贸易");
        info.setRegisteredAddress("上海市浦东新区张江高科技园区博云路" + (100 + random.nextInt(900)) + "号");
        info.setEnterpriseType("有限责任公司(自然人投资或控股)");
        info.setBusinessStatus("存续");

        Map<String, Object> matchScores;
        if (licenseNo.startsWith("T")) {
            info.setLegalPersonName("不匹配-" + merchant.getLegalPersonName());
            info.setMatchScore(new BigDecimal("45.0"));
            matchScores = buildMatchScores(95, 95, 10, 50);
        } else {
            int baseScore = 90 + random.nextInt(10);
            BigDecimal score = new BigDecimal(baseScore).add(new BigDecimal(random.nextDouble())).setScale(1, java.math.RoundingMode.HALF_UP);
            info.setMatchScore(score);
            matchScores = buildMatchScores(baseScore, 95, 95, 85);
        }
        info.setMatchScores(JsonUtils.toJsonString(matchScores));
        info.setVerifyRawResponse(JsonUtils.toJsonString(buildSandboxResp(true, null, info, matchScores)));

        long cost = System.currentTimeMillis() - startTime;
        log.info("[工商API-沙箱] 调用完成, requestId={}, cost={}ms, matchScore={}, status={}",
                requestId, cost, info.getMatchScore(), info.getBusinessStatus());
        return info;
    }

    private Map<String, Object> buildMatchScores(int nameScore, int licenseScore, int personScore, int statusScore) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("merchantName", nameScore);
        m.put("businessLicenseNo", licenseScore);
        m.put("legalPersonName", personScore);
        m.put("businessStatus", statusScore);
        return m;
    }

    private Map<String, Object> buildSandboxResp(boolean success, String message, BusinessInfoDTO info, Map<String, Object> matchScores) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", success);
        resp.put("message", message);
        resp.put("requestId", info.getVerifyRequestId());
        resp.put("sandbox", true);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("merchantName", info.getMerchantName());
        data.put("legalPersonName", info.getLegalPersonName());
        data.put("registeredCapital", info.getRegisteredCapital());
        data.put("establishmentDate", info.getEstablishmentDate());
        data.put("businessScope", info.getBusinessScope());
        data.put("registeredAddress", info.getRegisteredAddress());
        data.put("enterpriseType", info.getEnterpriseType());
        data.put("businessStatus", info.getBusinessStatus());
        data.put("matchScore", info.getMatchScore());
        data.put("matchScores", matchScores);
        data.put("vendor", SANDBOX_VENDOR);
        resp.put("data", data);
        return resp;
    }

    public BusinessVerifyResult verifyBusiness(MerchantInfo merchant, BusinessInfoDTO businessInfo) {
        BusinessVerifyResult result = new BusinessVerifyResult();
        result.setVerifyId(IdUtil.fastSimpleUUID());
        result.setVerifyTime(LocalDateTime.now());
        result.setBusinessInfo(businessInfo);
        result.setVerifiedBy(VERIFIED_BY_AUTO);
        result.setVerifyVendor(businessInfo != null ? businessInfo.getVerifyVendor() : SANDBOX_VENDOR);
        result.setVerifySource(businessInfo != null ? businessInfo.getVerifySource() : SANDBOX_VENDOR);
        result.setVerifyRequestId(businessInfo != null ? businessInfo.getVerifyRequestId() : null);

        if (businessInfo == null || businessInfo.getMatchScore() == null) {
            result.setPassed(false);
            result.setFailReason("工商信息查询失败");
            result.setDecisionReasons(Collections.singletonList("工商API未返回有效数据"));
            return result;
        }

        result.setMatchOverallScore(businessInfo.getMatchScore());
        result.setRawRequest(businessInfo.getVerifyRawRequest());
        result.setRawResponse(businessInfo.getVerifyRawResponse());
        result.setFallbackUsed(businessInfo.getFallbackUsed());

        List<String> failReasons = new ArrayList<>();
        List<String> decisionReasons = new ArrayList<>();
        boolean passed = true;

        if (businessInfo.getMatchScore().compareTo(new BigDecimal("60")) < 0) {
            passed = false;
            failReasons.add("工商信息匹配度不足60分(实际:" + businessInfo.getMatchScore() + ")");
            decisionReasons.add("综合匹配度" + businessInfo.getMatchScore() + " < 60门槛");
        } else {
            decisionReasons.add("综合匹配度" + businessInfo.getMatchScore() + " ≥ 60通过");
        }

        String status = businessInfo.getBusinessStatus();
        if (!"存续".equals(status) && !"开业".equals(status)) {
            passed = false;
            failReasons.add("企业状态异常: " + status);
            decisionReasons.add("经营状态=\"" + status + "\" 非存续/开业");
        } else {
            decisionReasons.add("经营状态=\"" + status + "\" 正常");
        }

        if (StrUtil.isNotBlank(businessInfo.getLegalPersonName())
                && !businessInfo.getLegalPersonName().equals(merchant.getLegalPersonName())) {
            passed = false;
            failReasons.add("法人姓名不匹配: 录入=\"" + merchant.getLegalPersonName() + "\" 工商=\"" + businessInfo.getLegalPersonName() + "\"");
            decisionReasons.add("法人姓名不匹配");
        } else {
            decisionReasons.add("法人姓名匹配");
        }

        if (StrUtil.isNotBlank(businessInfo.getMerchantName())
                && !businessInfo.getMerchantName().equals(merchant.getMerchantName())) {
            if (businessInfo.getMatchScore().compareTo(new BigDecimal("80")) < 0) {
                passed = false;
                failReasons.add("企业名称不匹配(匹配度<80)");
                decisionReasons.add("企业名称不匹配且综合匹配度<80");
            } else {
                decisionReasons.add("企业名称略有差异但综合匹配度≥80, 通过");
            }
        } else {
            decisionReasons.add("企业名称一致");
        }

        result.setPassed(passed);
        result.setFailReason(failReasons.isEmpty() ? null : String.join(";", failReasons));
        result.setDecisionReasons(decisionReasons);
        result.setDecisionJson(buildDecisionJson(result));
        result.setVerifyTime(LocalDateTime.now());

        log.info("工商核验决策完成, verifyId={}, passed={}, reasons={}", result.getVerifyId(), passed, decisionReasons);
        return result;
    }

    private String buildDecisionJson(BusinessVerifyResult result) {
        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("verifyId", result.getVerifyId());
        decision.put("passed", result.isPassed());
        decision.put("verifyTime", result.getVerifyTime().toString());
        decision.put("verifiedBy", result.getVerifiedBy());
        decision.put("verifyVendor", result.getVerifyVendor());
        decision.put("verifySource", result.getVerifySource());
        decision.put("verifyRequestId", result.getVerifyRequestId());
        decision.put("matchOverallScore", result.getMatchOverallScore());
        decision.put("decisionReasons", result.getDecisionReasons());
        decision.put("failReason", result.getFailReason());
        decision.put("fallbackUsed", result.isFallbackUsed());
        return JsonUtils.toJsonString(decision);
    }

    @lombok.Data
    public static class BusinessVerifyResult {
        private String verifyId;
        private boolean passed;
        private String failReason;
        private BusinessInfoDTO businessInfo;
        private String rawRequest;
        private String rawResponse;
        private BigDecimal matchOverallScore;
        private String verifiedBy;
        private String verifyVendor;
        private String verifySource;
        private String verifyRequestId;
        private boolean fallbackUsed;
        private List<String> decisionReasons;
        private String decisionJson;
        private LocalDateTime verifyTime;
    }
}
