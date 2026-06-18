package com.payhub.merchant.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payhub.common.enums.PayStatusEnum;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.common.utils.SignUtil;
import com.payhub.common.utils.Sm4Util;
import com.payhub.merchant.dto.MerchantConfigTestReport;
import com.payhub.merchant.dto.MerchantConfigTestRequest;
import com.payhub.merchant.dto.TestItemResult;
import com.payhub.merchant.entity.MerchantConfigTestLog;
import com.payhub.merchant.entity.MerchantInfo;
import com.payhub.merchant.mapper.MerchantConfigTestLogMapper;
import com.payhub.merchant.mapper.MerchantInfoMapper;
import com.payhub.merchant.service.MerchantConfigTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class MerchantConfigTestServiceImpl implements MerchantConfigTestService {

    private static final int HTTP_TIMEOUT = 10000;
    private static final String DEFAULT_MERCHANT_SECRET = "payhub_default_secret_key_2024";

    @Autowired
    private MerchantInfoMapper merchantInfoMapper;

    @Autowired
    private MerchantConfigTestLogMapper testLogMapper;

    private com.payhub.pay.mapper.MerchantPayConfigMapper merchantPayConfigMapper;

    @Autowired(required = false)
    public void setMerchantPayConfigMapper(com.payhub.pay.mapper.MerchantPayConfigMapper mapper) {
        this.merchantPayConfigMapper = mapper;
    }

    @Override
    public MerchantConfigTestReport runTest(MerchantConfigTestRequest request) {
        long startTime = System.currentTimeMillis();
        String logNo = "MCT" + System.currentTimeMillis() + RandomUtil.randomNumbers(4);

        MerchantInfo merchantInfo = getMerchantInfo(request.getMerchantNo());
        if (merchantInfo == null) {
            throw new BusinessException(ResultCode.MERCHANT_NOT_EXIST);
        }

        String signType = resolveSignType(request, merchantInfo);

        List<TestItemResult> items = new ArrayList<>();

        items.add(testMerchantInfo(merchantInfo));
        items.add(testSignMd5(merchantInfo));
        items.add(testSignRsa(merchantInfo));
        items.add(testSignSm2(merchantInfo));

        String callbackUrl = resolveCallbackUrl(request, merchantInfo);
        items.add(testConnectivity(callbackUrl));
        items.add(testCallbackNotify(request, merchantInfo, callbackUrl, signType));
        items.add(testSignatureVerification(merchantInfo));

        long totalTime = System.currentTimeMillis() - startTime;

        int passed = (int) items.stream().filter(item -> "PASS".equals(item.getStatus())).count();
        int failed = items.size() - passed;
        String overallStatus = failed == 0 ? "PASS" : (passed == 0 ? "FAIL" : "PARTIAL");
        String overallStatusDesc = failed == 0 ? "全部通过" : (passed == 0 ? "全部失败" : "部分通过");

        MerchantConfigTestReport report = new MerchantConfigTestReport();
        report.setMerchantNo(merchantInfo.getMerchantNo());
        report.setMerchantName(merchantInfo.getMerchantName());
        report.setTotalTests(items.size());
        report.setPassedTests(passed);
        report.setFailedTests(failed);
        report.setOverallStatus(overallStatus);
        report.setOverallStatusDesc(overallStatusDesc);
        report.setTotalTimeMs(totalTime);
        report.setTestTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        report.setItems(items);
        report.setSummary(buildSummary(report, items));

        saveTestLog(logNo, request, merchantInfo, report, callbackUrl, signType, items);

        log.info("商户配置一键测试完成: logNo={}, merchantNo={}, status={}, passed={}/{}, cost={}ms",
                logNo, merchantInfo.getMerchantNo(), overallStatus, passed, items.size(), totalTime);

        return report;
    }

    private void saveTestLog(String logNo, MerchantConfigTestRequest request, MerchantInfo merchantInfo,
                             MerchantConfigTestReport report, String callbackUrl, String signType,
                             List<TestItemResult> items) {
        try {
            MerchantConfigTestLog logEntity = new MerchantConfigTestLog();
            logEntity.setLogNo(logNo);
            logEntity.setMerchantNo(merchantInfo.getMerchantNo());
            logEntity.setMerchantName(merchantInfo.getMerchantName());
            logEntity.setTotalTests(report.getTotalTests());
            logEntity.setPassedTests(report.getPassedTests());
            logEntity.setFailedTests(report.getFailedTests());
            logEntity.setOverallStatus(report.getOverallStatus());
            logEntity.setOverallStatusDesc(report.getOverallStatusDesc());
            logEntity.setCallbackUrl(callbackUrl);
            logEntity.setSignType(signType);
            logEntity.setTotalTimeMs(report.getTotalTimeMs() != null ? report.getTotalTimeMs().intValue() : null);
            logEntity.setItemsJson(JSON.toJSONString(items));
            logEntity.setSummary(report.getSummary());
            testLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("保存商户配置测试记录失败", e);
        }
    }

    private TestItemResult testMerchantInfo(MerchantInfo merchant) {
        TestItemResult result = createItem("MERCHANT_INFO", "商户基本信息", "基础配置");
        long start = System.currentTimeMillis();

        try {
            result.setExpectedValue("商户状态正常，审核通过");
            boolean statusOk = merchant.getStatus() != null && merchant.getStatus() == 1;
            boolean auditOk = merchant.getAuditStatus() != null && merchant.getAuditStatus() == 1;

            String actual = String.format("状态:%s, 审核状态:%s",
                    statusOk ? "启用" : "禁用",
                    auditOk == null ? "未知" : (auditOk ? "通过" : "未通过"));
            result.setActualValue(actual);

            if (statusOk && auditOk) {
                markPass(result, "商户状态正常，已通过审核");
            } else {
                markFail(result, "商户状态异常", statusOk ? "请确保商户已通过审核" : "请先启用商户并完成审核");
            }
        } catch (Exception e) {
            markError(result, "检查商户信息异常: " + e.getMessage());
        }

        result.setDurationMs(System.currentTimeMillis() - start);
        return result;
    }

    private TestItemResult testSignMd5(MerchantInfo merchant) {
        TestItemResult result = createItem("SIGN_MD5", "MD5签名算法", "签名配置");
        long start = System.currentTimeMillis();

        try {
            result.setExpectedValue("MD5密钥已配置且签名正常");

            String md5Key = getMerchantMd5Secret(merchant);
            boolean hasKey = StrUtil.isNotBlank(md5Key) && !DEFAULT_MERCHANT_SECRET.equals(md5Key);

            if (hasKey) {
                TreeMap<String, Object> params = buildTestParams(merchant.getMerchantNo());
                String sign = SignUtil.sign(params, "MD5", md5Key, null, null);
                boolean valid = SignUtil.verify(params, "MD5", sign, md5Key, null, null);
                result.setActualValue("密钥已配置, 签名验证" + (valid ? "通过" : "失败"));

                if (valid) {
                    markPass(result, "MD5签名算法配置正确，签名与验证均正常");
                } else {
                    markFail(result, "MD5签名验证失败", "请检查密钥是否正确配置");
                }
            } else {
                result.setActualValue("未配置MD5密钥（使用默认密钥）");
                markWarning(result, "未配置商户专属MD5密钥，当前使用默认密钥",
                        "建议在商户密钥配置中设置专属MD5密钥");
            }
        } catch (Exception e) {
            markError(result, "MD5签名测试异常: " + e.getMessage());
        }

        result.setDurationMs(System.currentTimeMillis() - start);
        return result;
    }

    private TestItemResult testSignRsa(MerchantInfo merchant) {
        TestItemResult result = createItem("SIGN_RSA", "RSA签名算法", "签名配置");
        long start = System.currentTimeMillis();

        try {
            result.setExpectedValue("RSA密钥对已配置且签名正常");

            String privateKey = getMerchantRsaPrivateKey(merchant);
            String publicKey = merchant.getApiKeyRsaPublic();

            if (StrUtil.isNotBlank(privateKey) && StrUtil.isNotBlank(publicKey)) {
                TreeMap<String, Object> params = buildTestParams(merchant.getMerchantNo());
                String sign = SignUtil.sign(params, "RSA", null, privateKey, null);
                boolean valid = SignUtil.verify(params, "RSA", sign, null, publicKey, null);
                result.setActualValue("密钥对已配置, 签名验证" + (valid ? "通过" : "失败"));

                if (valid) {
                    markPass(result, "RSA签名算法配置正确，密钥对匹配正常");
                } else {
                    markFail(result, "RSA密钥对不匹配，签名验证失败", "请检查RSA公钥和私钥是否配对");
                }
            } else {
                result.setActualValue("未配置RSA密钥对");
                markWarning(result, "未配置RSA签名密钥", "如需使用RSA签名，请在商户密钥配置中设置RSA密钥对");
            }
        } catch (Exception e) {
            markError(result, "RSA签名测试异常: " + e.getMessage());
        }

        result.setDurationMs(System.currentTimeMillis() - start);
        return result;
    }

    private TestItemResult testSignSm2(MerchantInfo merchant) {
        TestItemResult result = createItem("SIGN_SM2", "SM2国密签名", "签名配置");
        long start = System.currentTimeMillis();

        try {
            result.setExpectedValue("SM2密钥对已配置且签名正常");

            String privateKey = getMerchantSm2PrivateKey(merchant);
            String publicKey = merchant.getApiKeySm2Public();

            if (StrUtil.isNotBlank(privateKey) && StrUtil.isNotBlank(publicKey)) {
                TreeMap<String, Object> params = buildTestParams(merchant.getMerchantNo());
                String sign = SignUtil.sign(params, "SM2", null, null, privateKey);
                boolean valid = SignUtil.verify(params, "SM2", sign, null, null, publicKey);
                result.setActualValue("密钥对已配置, 签名验证" + (valid ? "通过" : "失败"));

                if (valid) {
                    markPass(result, "SM2国密签名算法配置正确，公私钥匹配正常");
                } else {
                    markFail(result, "SM2公私钥不匹配，签名验证失败", "请检查SM2公钥和私钥是否配对");
                }
            } else if (StrUtil.isNotBlank(privateKey)) {
                result.setActualValue("已配置私钥，公钥未配置");
                markWarning(result, "SM2公钥未配置，验签无法完整验证",
                        "建议配置SM2公钥以完成完整验签流程");
            } else {
                result.setActualValue("未配置SM2密钥");
                markWarning(result, "未配置SM2国密签名密钥",
                        "如需使用国密签名，请在商户密钥配置中设置SM2密钥");
            }
        } catch (Exception e) {
            markError(result, "SM2签名测试异常: " + e.getMessage());
        }

        result.setDurationMs(System.currentTimeMillis() - start);
        return result;
    }

    private TestItemResult testConnectivity(String callbackUrl) {
        TestItemResult result = createItem("CONNECTIVITY", "回调地址连通性", "网络连通性");
        long start = System.currentTimeMillis();

        if (StrUtil.isBlank(callbackUrl)) {
            result.setExpectedValue("回调地址已配置");
            result.setActualValue("未配置回调地址");
            markFail(result, "未配置回调地址，无法进行连通性测试",
                    "请在商户支付配置中设置异步通知地址(notify_url)");
            result.setDurationMs(System.currentTimeMillis() - start);
            return result;
        }

        try {
            result.setExpectedValue("回调地址可访问，HTTP 2xx/3xx 响应（非404）");

            Map<String, Object> probeBody = new LinkedHashMap<>();
            probeBody.put("probe", "payhub_connectivity_test");
            probeBody.put("timestamp", System.currentTimeMillis());
            probeBody.put("nonce", UUID.randomUUID().toString().replace("-", "").substring(0, 16));

            HttpResponse response = HttpRequest.post(callbackUrl)
                    .body(JSON.toJSONString(probeBody), "application/json;charset=UTF-8")
                    .timeout(HTTP_TIMEOUT)
                    .header("User-Agent", "PayHub-Connectivity-Test")
                    .header("X-Test-Request", "true")
                    .header("X-Probe-Mode", "connectivity")
                    .execute();

            int status = response.getStatus();
            long duration = System.currentTimeMillis() - start;

            result.setActualValue(String.format("HTTP %d, 耗时%dms", status, duration));
            result.setDetail("探测请求体:\n" + JSON.toJSONString(probeBody, true)
                    + "\n\n响应体:\n" + (response.body() != null ? response.body() : "(空)"));

            if (status == 404) {
                markFail(result, String.format("回调地址不存在（HTTP 404）", status),
                        "请确认回调地址URL是否正确，或服务器端是否已部署该接口");
            } else if (status >= 200 && status < 400) {
                markPass(result, String.format("回调地址可正常访问（HTTP %d）", status));
            } else if (status >= 400 && status < 500) {
                markWarning(result, String.format("回调地址返回客户端错误（HTTP %d）", status),
                        "请确认请求格式是否符合商户接口要求，或参数校验规则");
            } else {
                markFail(result, String.format("回调地址服务器异常（HTTP %d）", status),
                        "请检查商户服务是否正常运行、日志是否报错");
            }
        } catch (Exception e) {
            result.setActualValue("连接失败: " + e.getMessage());
            markFail(result, "无法连接到回调地址: " + e.getMessage(),
                    "请检查回调地址是否正确、网络是否通畅、防火墙是否开放端口");
        }

        result.setDurationMs(System.currentTimeMillis() - start);
        return result;
    }

    private TestItemResult testCallbackNotify(MerchantConfigTestRequest request, MerchantInfo merchant,
                                              String callbackUrl, String signType) {
        TestItemResult result = createItem("CALLBACK_NOTIFY", "支付回调通知", "回调功能");
        long start = System.currentTimeMillis();

        if (StrUtil.isBlank(callbackUrl)) {
            result.setExpectedValue("回调通知发送成功，商户正确响应");
            result.setActualValue("未配置回调地址");
            markFail(result, "未配置回调地址",
                    "请在商户支付配置中设置异步通知地址(notify_url)");
            result.setDurationMs(System.currentTimeMillis() - start);
            return result;
        }

        try {
            result.setExpectedValue("商户返回 2xx 状态码且响应体包含成功标识");

            String orderNo = OrderNoGenerator.generate();
            TreeMap<String, Object> params = buildNotifyParams(merchant.getMerchantNo(), orderNo, request);

            String md5Key = "MD5".equalsIgnoreCase(signType) ? getMerchantMd5Secret(merchant) : null;
            String rsaPrivateKey = "RSA".equalsIgnoreCase(signType) ? getMerchantRsaPrivateKey(merchant) : null;
            String sm2PrivateKey = "SM2".equalsIgnoreCase(signType) ? getMerchantSm2PrivateKey(merchant) : null;

            String sign = SignUtil.sign(params, signType, md5Key, rsaPrivateKey, sm2PrivateKey);
            params.put("sign", sign);
            params.put("signType", signType);

            String requestBody = JSON.toJSONString(params);

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Content-Type", "application/json;charset=UTF-8");
            headers.put("X-Callback-Test", "true");
            headers.put("X-Test-Order", orderNo);
            headers.put("X-Sign-Type", signType);

            HttpResponse response = HttpRequest.post(callbackUrl)
                    .body(requestBody, "application/json;charset=UTF-8")
                    .timeout(HTTP_TIMEOUT)
                    .addHeaders(headers)
                    .execute();

            int status = response.getStatus();
            String responseBody = response.body();
            long duration = System.currentTimeMillis() - start;

            boolean httpOk = status >= 200 && status < 300;
            boolean hasSuccess = responseBody != null &&
                    (responseBody.toLowerCase().contains("success") ||
                            responseBody.toLowerCase().contains("\"code\":0") ||
                            responseBody.toLowerCase().contains("\"code\":200") ||
                            responseBody.toLowerCase().contains("\"status\":\"success\"") ||
                            responseBody.toLowerCase().contains("ok"));

            result.setActualValue(String.format("HTTP %d, 耗时%dms", status, duration));
            result.setDetail("请求头:\n" + JSON.toJSONString(headers, true)
                    + "\n\n请求体:\n" + requestBody
                    + "\n\n响应体:\n" + (responseBody != null ? responseBody : "(空)"));

            if (status == 404) {
                markFail(result, "回调地址不存在（HTTP 404）",
                        "请确认异步通知地址是否正确，或接口是否已实现");
            } else if (httpOk && hasSuccess) {
                markPass(result, "回调通知发送成功，商户正确响应");
            } else if (httpOk) {
                markWarning(result, "回调通知HTTP成功，但未检测到成功响应标识",
                        "请确保商户响应体包含 success/OK 或 code=0/200 等约定成功标识");
            } else {
                markFail(result, String.format("回调通知发送失败（HTTP %d）", status),
                        "请检查回调地址、商户服务状态和回调处理逻辑");
            }
        } catch (Exception e) {
            result.setActualValue("发送失败: " + e.getMessage());
            markFail(result, "回调通知发送异常: " + e.getMessage(),
                    "请检查回调地址、网络连接和商户服务状态");
        }

        result.setDurationMs(System.currentTimeMillis() - start);
        return result;
    }

    private TestItemResult testSignatureVerification(MerchantInfo merchant) {
        TestItemResult result = createItem("SIGN_VERIFY", "签名自验证", "签名安全");
        long start = System.currentTimeMillis();

        try {
            result.setExpectedValue("所有已配置的签名算法均可正常签名和验签");

            List<String> passedAlgorithms = new ArrayList<>();
            List<String> failedAlgorithms = new ArrayList<>();
            List<String> notConfigured = new ArrayList<>();

            String md5Key = getMerchantMd5Secret(merchant);
            if (StrUtil.isNotBlank(md5Key) && !DEFAULT_MERCHANT_SECRET.equals(md5Key)) {
                TreeMap<String, Object> params = buildTestParams(merchant.getMerchantNo());
                String sign = SignUtil.sign(params, "MD5", md5Key, null, null);
                if (SignUtil.verify(params, "MD5", sign, md5Key, null, null)) {
                    passedAlgorithms.add("MD5");
                } else {
                    failedAlgorithms.add("MD5");
                }
            } else {
                notConfigured.add("MD5(默认)");
            }

            String rsaPri = getMerchantRsaPrivateKey(merchant);
            String rsaPub = merchant.getApiKeyRsaPublic();
            if (StrUtil.isNotBlank(rsaPri) && StrUtil.isNotBlank(rsaPub)) {
                TreeMap<String, Object> params = buildTestParams(merchant.getMerchantNo());
                String sign = SignUtil.sign(params, "RSA", null, rsaPri, null);
                if (SignUtil.verify(params, "RSA", sign, null, rsaPub, null)) {
                    passedAlgorithms.add("RSA");
                } else {
                    failedAlgorithms.add("RSA");
                }
            } else if (StrUtil.isNotBlank(rsaPri) || StrUtil.isNotBlank(rsaPub)) {
                notConfigured.add("RSA(不完整)");
            }

            String sm2Pri = getMerchantSm2PrivateKey(merchant);
            String sm2Pub = merchant.getApiKeySm2Public();
            if (StrUtil.isNotBlank(sm2Pri) && StrUtil.isNotBlank(sm2Pub)) {
                TreeMap<String, Object> params = buildTestParams(merchant.getMerchantNo());
                String sign = SignUtil.sign(params, "SM2", null, null, sm2Pri);
                if (SignUtil.verify(params, "SM2", sign, null, null, sm2Pub)) {
                    passedAlgorithms.add("SM2");
                } else {
                    failedAlgorithms.add("SM2");
                }
            } else if (StrUtil.isNotBlank(sm2Pri) || StrUtil.isNotBlank(sm2Pub)) {
                notConfigured.add("SM2(不完整)");
            }

            String actual = String.format("通过:%s, 失败:%s, 未配置:%s",
                    passedAlgorithms.isEmpty() ? "无" : String.join(",", passedAlgorithms),
                    failedAlgorithms.isEmpty() ? "无" : String.join(",", failedAlgorithms),
                    notConfigured.isEmpty() ? "无" : String.join(",", notConfigured));
            result.setActualValue(actual);

            if (failedAlgorithms.isEmpty() && !passedAlgorithms.isEmpty()) {
                markPass(result, "所有已配置的签名算法均通过自验证");
            } else if (!passedAlgorithms.isEmpty()) {
                markWarning(result, "部分签名算法验证失败",
                        "请检查以下算法的密钥配置: " + String.join(", ", failedAlgorithms));
            } else {
                markWarning(result, "未配置可用的签名密钥", "请至少配置一种签名算法的完整密钥对");
            }
        } catch (Exception e) {
            markError(result, "签名自验证异常: " + e.getMessage());
        }

        result.setDurationMs(System.currentTimeMillis() - start);
        return result;
    }

    private TestItemResult createItem(String code, String name, String category) {
        TestItemResult item = new TestItemResult();
        item.setItemCode(code);
        item.setItemName(name);
        item.setItemCategory(category);
        item.setStatus("PENDING");
        item.setStatusDesc("待测试");
        return item;
    }

    private void markPass(TestItemResult item, String message) {
        item.setStatus("PASS");
        item.setStatusDesc("通过");
        item.setMessage(message);
    }

    private void markFail(TestItemResult item, String message, String suggestion) {
        item.setStatus("FAIL");
        item.setStatusDesc("失败");
        item.setMessage(message);
        item.setSuggestion(suggestion);
    }

    private void markWarning(TestItemResult item, String message, String suggestion) {
        item.setStatus("WARN");
        item.setStatusDesc("警告");
        item.setMessage(message);
        item.setSuggestion(suggestion);
    }

    private void markError(TestItemResult item, String message) {
        item.setStatus("ERROR");
        item.setStatusDesc("异常");
        item.setMessage(message);
    }

    private String buildSummary(MerchantConfigTestReport report, List<TestItemResult> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("测试完成，共").append(report.getTotalTests()).append("项测试，")
                .append("通过").append(report.getPassedTests()).append("项，")
                .append("失败/警告").append(report.getFailedTests()).append("项。\n\n");

        List<String> fails = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (TestItemResult item : items) {
            if ("FAIL".equals(item.getStatus()) || "ERROR".equals(item.getStatus())) {
                fails.add("【" + item.getItemName() + "】" + item.getMessage());
            } else if ("WARN".equals(item.getStatus())) {
                warnings.add("【" + item.getItemName() + "】" + item.getMessage());
            }
        }

        if (!fails.isEmpty()) {
            sb.append("存在以下问题需要处理：\n");
            for (String f : fails) {
                sb.append("  ✗ ").append(f).append("\n");
            }
            sb.append("\n");
        }

        if (!warnings.isEmpty()) {
            sb.append("建议优化以下配置：\n");
            for (String w : warnings) {
                sb.append("  ⚠ ").append(w).append("\n");
            }
        }

        if (fails.isEmpty() && warnings.isEmpty()) {
            sb.append("所有测试项均已通过，商户配置状态良好！");
        }

        return sb.toString();
    }

    private MerchantInfo getMerchantInfo(String merchantNo) {
        if (StrUtil.isBlank(merchantNo)) {
            return null;
        }
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getMerchantNo, merchantNo)
                .last("LIMIT 1");
        return merchantInfoMapper.selectOne(wrapper);
    }

    private String resolveCallbackUrl(MerchantConfigTestRequest request, MerchantInfo merchantInfo) {
        if (StrUtil.isNotBlank(request.getCallbackUrl())) {
            return request.getCallbackUrl();
        }
        if (merchantPayConfigMapper != null && merchantInfo != null) {
            try {
                LambdaQueryWrapper<com.payhub.pay.entity.MerchantPayConfig> w =
                        new LambdaQueryWrapper<>();
                w.eq(com.payhub.pay.entity.MerchantPayConfig::getMerchantNo, merchantInfo.getMerchantNo())
                        .eq(com.payhub.pay.entity.MerchantPayConfig::getStatus, 1)
                        .orderByAsc(com.payhub.pay.entity.MerchantPayConfig::getPriority)
                        .last("LIMIT 1");
                com.payhub.pay.entity.MerchantPayConfig cfg = merchantPayConfigMapper.selectOne(w);
                if (cfg != null) {
                    try {
                        java.lang.reflect.Field f = com.payhub.pay.entity.MerchantPayConfig.class
                                .getDeclaredField("notifyUrl");
                        f.setAccessible(true);
                        Object notifyUrl = f.get(cfg);
                        if (notifyUrl != null && StrUtil.isNotBlank(String.valueOf(notifyUrl))) {
                            return String.valueOf(notifyUrl);
                        }
                    } catch (NoSuchFieldException ignore) {
                    }
                }
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    private String resolveSignType(MerchantConfigTestRequest request, MerchantInfo merchantInfo) {
        if (StrUtil.isNotBlank(request.getSignType())) {
            return request.getSignType().toUpperCase();
        }
        if (merchantInfo != null) {
            if (StrUtil.isNotBlank(merchantInfo.getApiKeyMd5())) {
                return "MD5";
            }
            if (StrUtil.isNotBlank(merchantInfo.getApiKeyRsaPrivate())) {
                return "RSA";
            }
            if (StrUtil.isNotBlank(merchantInfo.getApiKeySm2Private())) {
                return "SM2";
            }
        }
        return "MD5";
    }

    private String getMerchantMd5Secret(MerchantInfo merchantInfo) {
        try {
            if (merchantInfo != null && StrUtil.isNotBlank(merchantInfo.getApiKeyMd5())) {
                String secret = Sm4Util.decrypt(merchantInfo.getApiKeyMd5());
                if (StrUtil.isNotBlank(secret)) {
                    return secret;
                }
            }
        } catch (Exception e) {
            log.warn("获取商户MD5密钥失败", e);
        }
        return DEFAULT_MERCHANT_SECRET;
    }

    private String getMerchantRsaPrivateKey(MerchantInfo merchantInfo) {
        try {
            if (merchantInfo != null && StrUtil.isNotBlank(merchantInfo.getApiKeyRsaPrivate())) {
                String key = Sm4Util.decrypt(merchantInfo.getApiKeyRsaPrivate());
                if (StrUtil.isNotBlank(key)) {
                    return key;
                }
            }
        } catch (Exception e) {
            log.warn("获取商户RSA私钥失败", e);
        }
        return null;
    }

    private String getMerchantSm2PrivateKey(MerchantInfo merchantInfo) {
        try {
            if (merchantInfo != null && StrUtil.isNotBlank(merchantInfo.getApiKeySm2Private())) {
                String key = Sm4Util.decrypt(merchantInfo.getApiKeySm2Private());
                if (StrUtil.isNotBlank(key)) {
                    return key;
                }
            }
        } catch (Exception e) {
            log.warn("获取商户SM2私钥失败", e);
        }
        return null;
    }

    private TreeMap<String, Object> buildTestParams(String merchantNo) {
        TreeMap<String, Object> params = new TreeMap<>();
        params.put("merchantNo", merchantNo);
        params.put("orderNo", "TEST" + System.currentTimeMillis());
        params.put("amount", 100);
        params.put("status", "SUCCESS");
        params.put("timestamp", System.currentTimeMillis());
        params.put("nonceStr", "test_nonce_123456");
        return params;
    }

    private TreeMap<String, Object> buildNotifyParams(String merchantNo, String orderNo,
                                                      MerchantConfigTestRequest request) {
        TreeMap<String, Object> params = new TreeMap<>();
        params.put("merchantNo", merchantNo);
        params.put("orderNo", orderNo);
        params.put("merchantOrderNo", "TEST_MOCK_" + System.currentTimeMillis());
        params.put("payChannel", "PAYHUB");
        params.put("payType", "TEST");
        params.put("payStatus", PayStatusEnum.SUCCESS.getCode());
        BigDecimal amount = StrUtil.isNotBlank(request.getTestAmount())
                ? new BigDecimal(request.getTestAmount())
                : new BigDecimal("100.00");
        params.put("payAmount", amount);
        params.put("payTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("channelTradeNo", "CT" + System.currentTimeMillis());
        params.put("callbackType", "PAY");
        params.put("timestamp", System.currentTimeMillis());
        params.put("nonceStr", UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        return params;
    }
}
