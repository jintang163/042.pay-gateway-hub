package com.payhub.merchant.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payhub.common.enums.PayStatusEnum;
import com.payhub.common.enums.RefundStatusEnum;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.common.utils.SignUtil;
import com.payhub.common.utils.Sm4Util;
import com.payhub.merchant.dto.CallbackSimulateRequest;
import com.payhub.merchant.dto.CallbackSimulateVO;
import com.payhub.merchant.dto.SignCodeExampleRequest;
import com.payhub.merchant.dto.SignCodeExampleVO;
import com.payhub.merchant.entity.CallbackSimulateLog;
import com.payhub.merchant.entity.MerchantInfo;
import com.payhub.merchant.mapper.CallbackSimulateLogMapper;
import com.payhub.merchant.mapper.MerchantInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class CallbackSimulateService {

    private static final String DEFAULT_MERCHANT_SECRET = "payhub_default_secret_key_2024";
    private static final int HTTP_TIMEOUT = 30000;

    @Autowired
    private CallbackSimulateLogMapper callbackSimulateLogMapper;

    @Autowired
    private MerchantInfoMapper merchantInfoMapper;

    public CallbackSimulateVO simulate(CallbackSimulateRequest request) {
        MerchantInfo merchantInfo = getMerchantInfo(request.getMerchantNo());
        if (merchantInfo == null) {
            throw new BusinessException(ResultCode.CALLBACK_TEST_FAIL, "商户不存在");
        }

        String logNo = "CS" + System.currentTimeMillis() + RandomUtil.randomNumbers(4);
        String orderNo = StrUtil.isNotBlank(request.getOrderNo())
                ? request.getOrderNo()
                : OrderNoGenerator.generate();

        String callbackUrl = resolveCallbackUrl(request, merchantInfo);
        if (StrUtil.isBlank(callbackUrl)) {
            throw new BusinessException(ResultCode.CALLBACK_TEST_FAIL, "回调地址不能为空");
        }

        TreeMap<String, Object> params = buildNotifyParams(request, merchantInfo, orderNo);
        String signType = resolveSignType(request, merchantInfo);

        String md5Key = null;
        String rsaPrivateKey = null;
        String sm2PrivateKey = null;

        if ("MD5".equalsIgnoreCase(signType)) {
            md5Key = getMerchantMd5Secret(merchantInfo);
        } else if ("RSA".equalsIgnoreCase(signType)) {
            rsaPrivateKey = getMerchantRsaPrivateKey(merchantInfo);
            if (StrUtil.isBlank(rsaPrivateKey)) {
                SignUtil.RsaKeyPair pair = SignUtil.generateRsaKeyPair();
                rsaPrivateKey = pair.getPrivateKey();
            }
        } else if ("SM2".equalsIgnoreCase(signType)) {
            sm2PrivateKey = getMerchantSm2PrivateKey(merchantInfo);
            if (StrUtil.isBlank(sm2PrivateKey)) {
                SignUtil.Sm2KeyPair pair = SignUtil.generateSm2KeyPair();
                sm2PrivateKey = pair.getPrivateKey();
            }
        }

        String sign = SignUtil.sign(params, signType, md5Key, rsaPrivateKey, sm2PrivateKey);
        params.put("sign", sign);
        params.put("signType", signType);

        String requestBody;
        if (StrUtil.isNotBlank(request.getCustomRequestBody())) {
            JSONObject customJson = JSON.parseObject(request.getCustomRequestBody());
            customJson.put("sign", sign);
            customJson.put("signType", signType);
            requestBody = customJson.toJSONString();
        } else {
            requestBody = JSON.toJSONString(params);
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("X-Callback-Simulate", "true");
        headers.put("X-Callback-Type", request.getCallbackType());
        headers.put("X-Request-Id", logNo);

        HttpResponseResult httpResult = doPost(callbackUrl, requestBody, headers);

        CallbackSimulateLog logEntity = new CallbackSimulateLog();
        logEntity.setLogNo(logNo);
        logEntity.setMerchantNo(request.getMerchantNo());
        logEntity.setMerchantName(merchantInfo.getMerchantName());
        logEntity.setOrderNo(orderNo);
        logEntity.setCallbackUrl(callbackUrl);
        logEntity.setCallbackType(request.getCallbackType());
        logEntity.setSimulateStatus(request.getSimulateStatus());
        logEntity.setSignType(signType);
        logEntity.setRequestHeaders(JSON.toJSONString(headers));
        logEntity.setRequestBody(requestBody);
        logEntity.setResponseHttpStatus(httpResult.httpStatus);
        logEntity.setResponseBody(httpResult.responseBody);
        logEntity.setResponseTimeMs(httpResult.responseTimeMs);
        logEntity.setCallbackStatus(determineCallbackStatus(httpResult));
        logEntity.setRetryCount(0);
        logEntity.setRemark(request.getRemark());
        callbackSimulateLogMapper.insert(logEntity);

        log.info("回调模拟完成: logNo={}, callbackStatus={}, httpStatus={}, responseTimeMs={}",
                logNo, logEntity.getCallbackStatus(), httpResult.httpStatus, httpResult.responseTimeMs);

        return convertToVO(logEntity);
    }

    public IPage<CallbackSimulateVO> listPage(Long current, Long size, Map<String, Object> params) {
        Page<CallbackSimulateLog> page = new Page<>(current, size);
        LambdaQueryWrapper<CallbackSimulateLog> wrapper = new LambdaQueryWrapper<>();

        if (params != null) {
            String merchantNo = (String) params.get("merchantNo");
            if (StrUtil.isNotBlank(merchantNo)) {
                wrapper.eq(CallbackSimulateLog::getMerchantNo, merchantNo);
            }
            String callbackType = (String) params.get("callbackType");
            if (StrUtil.isNotBlank(callbackType)) {
                wrapper.eq(CallbackSimulateLog::getCallbackType, callbackType);
            }
            String simulateStatus = (String) params.get("simulateStatus");
            if (StrUtil.isNotBlank(simulateStatus)) {
                wrapper.eq(CallbackSimulateLog::getSimulateStatus, simulateStatus);
            }
            Object callbackStatusObj = params.get("callbackStatus");
            if (callbackStatusObj != null && StrUtil.isNotBlank(String.valueOf(callbackStatusObj))) {
                wrapper.eq(CallbackSimulateLog::getCallbackStatus, Integer.valueOf(String.valueOf(callbackStatusObj)));
            }
        }

        wrapper.orderByDesc(CallbackSimulateLog::getCreatedAt);

        IPage<CallbackSimulateLog> logPage = callbackSimulateLogMapper.selectPage(page, wrapper);

        Page<CallbackSimulateVO> voPage = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        voPage.setPages(logPage.getPages());
        voPage.setRecords(logPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(java.util.stream.Collectors.toList()));

        return voPage;
    }

    public CallbackSimulateVO resend(String logNo) {
        LambdaQueryWrapper<CallbackSimulateLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CallbackSimulateLog::getLogNo, logNo).last("LIMIT 1");
        CallbackSimulateLog logEntity = callbackSimulateLogMapper.selectOne(wrapper);
        if (logEntity == null) {
            throw new BusinessException(ResultCode.CALLBACK_TEST_FAIL, "回调模拟记录不存在");
        }

        Map<String, String> headers;
        if (StrUtil.isNotBlank(logEntity.getRequestHeaders())) {
            headers = new LinkedHashMap<>();
            try {
                JSONObject headerJson = JSON.parseObject(logEntity.getRequestHeaders());
                for (String k : headerJson.keySet()) {
                    headers.put(k, String.valueOf(headerJson.get(k)));
                }
            } catch (Exception e) {
                headers = buildDefaultHeaders(logNo, logEntity.getCallbackType());
            }
        } else {
            headers = buildDefaultHeaders(logNo, logEntity.getCallbackType());
        }

        HttpResponseResult httpResult = doPost(logEntity.getCallbackUrl(), logEntity.getRequestBody(), headers);

        int newRetryCount = (logEntity.getRetryCount() == null ? 0 : logEntity.getRetryCount()) + 1;
        int callbackStatus = determineCallbackStatus(httpResult);

        logEntity.setRetryCount(newRetryCount);
        logEntity.setResponseHttpStatus(httpResult.httpStatus);
        logEntity.setResponseBody(httpResult.responseBody);
        logEntity.setResponseTimeMs(httpResult.responseTimeMs);
        logEntity.setCallbackStatus(callbackStatus);
        callbackSimulateLogMapper.updateById(logEntity);

        log.info("回调重发完成: logNo={}, retryCount={}, callbackStatus={}, httpStatus={}, responseTimeMs={}",
                logNo, newRetryCount, callbackStatus, httpResult.httpStatus, httpResult.responseTimeMs);

        return convertToVO(logEntity);
    }

    public CallbackSimulateVO getByLogNo(String logNo) {
        LambdaQueryWrapper<CallbackSimulateLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CallbackSimulateLog::getLogNo, logNo).last("LIMIT 1");
        CallbackSimulateLog logEntity = callbackSimulateLogMapper.selectOne(wrapper);
        if (logEntity == null) {
            throw new BusinessException(ResultCode.CALLBACK_TEST_FAIL, "回调模拟记录不存在");
        }
        return convertToVO(logEntity);
    }

    public SignCodeExampleVO generateSignCode(SignCodeExampleRequest request) {
        String language = request.getLanguage().toUpperCase();
        String signType = request.getSignType().toUpperCase();
        String code = generateCode(language, signType, request.getParams(), request.getKey());
        String description = language + " " + signType + " 签名示例代码";

        SignCodeExampleVO vo = new SignCodeExampleVO();
        vo.setLanguage(language);
        vo.setSignType(signType);
        vo.setCode(code);
        vo.setDescription(description);
        return vo;
    }

    private MerchantInfo getMerchantInfo(String merchantNo) {
        if (StrUtil.isBlank(merchantNo)) {
            return null;
        }
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getMerchantNo, merchantNo)
                .eq(MerchantInfo::getStatus, 1)
                .last("LIMIT 1");
        return merchantInfoMapper.selectOne(wrapper);
    }

    private String resolveCallbackUrl(CallbackSimulateRequest request, MerchantInfo merchantInfo) {
        if (StrUtil.isNotBlank(request.getCallbackUrl())) {
            return request.getCallbackUrl();
        }
        try {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.payhub.pay.entity.MerchantPayConfig> w =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            w.eq(com.payhub.pay.entity.MerchantPayConfig::getMerchantNo, merchantInfo.getMerchantNo())
                    .eq(com.payhub.pay.entity.MerchantPayConfig::getStatus, 1)
                    .orderByAsc(com.payhub.pay.entity.MerchantPayConfig::getPriority)
                    .last("LIMIT 1");
            try {
                com.payhub.pay.entity.MerchantPayConfig cfg = getMerchantPayConfigMapper().selectOne(w);
                if (cfg != null) {
                    // no notify_url on entity, fall through to default
                }
            } catch (Exception ignore) {
            }
        } catch (Exception ignore) {
        }
        if (StrUtil.isBlank(request.getCallbackUrl()) && merchantInfo != null) {
            String phone = merchantInfo.getContactPhone();
            if (phone != null && !phone.isEmpty()) {
                return null;
            }
        }
        return request.getCallbackUrl();
    }

    private com.payhub.pay.mapper.MerchantPayConfigMapper merchantPayConfigMapper;

    private com.payhub.pay.mapper.MerchantPayConfigMapper getMerchantPayConfigMapper() {
        return merchantPayConfigMapper;
    }

    @Autowired(required = false)
    public void setMerchantPayConfigMapper(com.payhub.pay.mapper.MerchantPayConfigMapper mapper) {
        this.merchantPayConfigMapper = mapper;
    }

    private TreeMap<String, Object> buildNotifyParams(CallbackSimulateRequest request, MerchantInfo merchantInfo, String orderNo) {
        TreeMap<String, Object> params = new TreeMap<>();
        params.put("merchantNo", request.getMerchantNo());
        params.put("orderNo", orderNo);
        params.put("merchantOrderNo", request.getOrderNo() != null ? request.getOrderNo() : orderNo);
        params.put("payChannel", "PAYHUB");
        params.put("payType", "SIMULATE");

        boolean isPay = "PAY".equalsIgnoreCase(request.getCallbackType());
        if (isPay) {
            Integer payStatus = "SUCCESS".equalsIgnoreCase(request.getSimulateStatus())
                    ? PayStatusEnum.SUCCESS.getCode()
                    : PayStatusEnum.FAIL.getCode();
            params.put("payStatus", payStatus);
        } else {
            Integer refundStatus = "SUCCESS".equalsIgnoreCase(request.getSimulateStatus())
                    ? RefundStatusEnum.SUCCESS.getCode()
                    : RefundStatusEnum.FAIL.getCode();
            params.put("refundStatus", refundStatus);
            params.put("refundNo", "RF" + System.currentTimeMillis());
        }

        BigDecimal amount = request.getAmount() != null ? new BigDecimal(request.getAmount()) : new BigDecimal(10000);
        params.put(isPay ? "payAmount" : "refundAmount", amount);

        if ("SUCCESS".equalsIgnoreCase(request.getSimulateStatus())) {
            LocalDateTime now = LocalDateTime.now();
            String timeStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            params.put(isPay ? "payTime" : "refundTime", timeStr);
            params.put("channelTradeNo", "CT" + System.currentTimeMillis() + RandomUtil.randomNumbers(4));
        }

        params.put("callbackType", request.getCallbackType());
        params.put("status", request.getSimulateStatus());
        params.put("timestamp", System.currentTimeMillis());
        params.put("nonceStr", RandomUtil.randomString(16));

        return params;
    }

    private String resolveSignType(CallbackSimulateRequest request, MerchantInfo merchantInfo) {
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
            log.warn("获取商户MD5密钥失败, merchantNo={}, 使用默认密钥", merchantInfo == null ? null : merchantInfo.getMerchantNo(), e);
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
            log.warn("获取商户RSA私钥失败, merchantNo={}", merchantInfo == null ? null : merchantInfo.getMerchantNo(), e);
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
            log.warn("获取商户SM2私钥失败, merchantNo={}", merchantInfo == null ? null : merchantInfo.getMerchantNo(), e);
        }
        return null;
    }

    private Map<String, String> buildDefaultHeaders(String logNo, String callbackType) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("X-Callback-Simulate", "true");
        headers.put("X-Callback-Type", callbackType);
        headers.put("X-Request-Id", logNo);
        return headers;
    }

    private static class HttpResponseResult {
        Integer httpStatus;
        String responseBody;
        int responseTimeMs;

        HttpResponseResult(Integer httpStatus, String responseBody, int responseTimeMs) {
            this.httpStatus = httpStatus;
            this.responseBody = responseBody;
            this.responseTimeMs = responseTimeMs;
        }
    }

    private HttpResponseResult doPost(String url, String body, Map<String, String> headers) {
        long startTime = System.currentTimeMillis();
        Integer httpStatus = null;
        String responseBody = null;
        try {
            HttpRequest httpRequest = HttpRequest.post(url)
                    .body(body, "application/json;charset=UTF-8")
                    .timeout(HTTP_TIMEOUT);
            if (headers != null) {
                httpRequest.addHeaders(headers);
            }
            HttpResponse response = httpRequest.execute();
            httpStatus = response.getStatus();
            responseBody = response.body();
        } catch (Exception e) {
            log.error("HTTP POST请求异常: url={}", url, e);
            httpStatus = 0;
            responseBody = "REQUEST_ERROR: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
        int responseTimeMs = (int) (System.currentTimeMillis() - startTime);
        return new HttpResponseResult(httpStatus, responseBody, responseTimeMs);
    }

    private int determineCallbackStatus(HttpResponseResult result) {
        if (result.httpStatus == null) {
            return 2;
        }
        boolean success = result.httpStatus >= 200 && result.httpStatus < 300
                && StrUtil.isNotBlank(result.responseBody)
                && !result.responseBody.startsWith("REQUEST_ERROR");
        if (success) {
            return 1;
        }
        return 2;
    }

    private String generateCode(String language, String signType, Map<String, Object> params, String key) {
        switch (language) {
            case "JAVA":
                return generateJavaCode(signType, params, key);
            case "PHP":
                return generatePhpCode(signType, params, key);
            case "PYTHON":
                return generatePythonCode(signType, params, key);
            default:
                return generateJavaCode(signType, params, key);
        }
    }

    private String generateJavaCode(String signType, Map<String, Object> params, String key) {
        String paramStr = params != null ? formatParamsForCode(params) : "merchantNo=M100001&amount=100&orderNo=PG123456&status=SUCCESS&timestamp=1700000000000";
        String keyStr = StrUtil.isNotBlank(key) ? key : "your_md5_key";

        switch (signType) {
            case "RSA":
                return "import java.security.KeyFactory;\n"
                        + "import java.security.PrivateKey;\n"
                        + "import java.security.Signature;\n"
                        + "import java.util.Base64;\n"
                        + "import java.util.TreeMap;\n"
                        + "import java.util.Map;\n\n"
                        + "public class RsaSignDemo {\n"
                        + "    public static String sign(Map<String, Object> params, String privateKeyStr) throws Exception {\n"
                        + "        TreeMap<String, Object> sortedMap = new TreeMap<>(params);\n"
                        + "        StringBuilder sb = new StringBuilder();\n"
                        + "        for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {\n"
                        + "            if (entry.getValue() != null && !\"sign\".equals(entry.getKey()) && !\"signType\".equals(entry.getKey())) {\n"
                        + "                sb.append(entry.getKey()).append(\"=\").append(entry.getValue()).append(\"&\");\n"
                        + "            }\n"
                        + "        }\n"
                        + "        sb.deleteCharAt(sb.length() - 1);\n"
                        + "        byte[] keyBytes = Base64.getDecoder().decode(privateKeyStr);\n"
                        + "        java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);\n"
                        + "        PrivateKey privateKey = KeyFactory.getInstance(\"RSA\").generatePrivate(keySpec);\n"
                        + "        Signature signature = Signature.getInstance(\"SHA256WithRSA\");\n"
                        + "        signature.initSign(privateKey);\n"
                        + "        signature.update(sb.toString().getBytes(\"UTF-8\"));\n"
                        + "        return Base64.getEncoder().encodeToString(signature.sign());\n"
                        + "    }\n"
                        + "}";
            case "SM2":
                return "import org.bouncycastle.crypto.params.ECPrivateKeyParameters;\n"
                        + "import org.bouncycastle.crypto.signers.SM2Signer;\n"
                        + "import cn.hutool.core.util.HexUtil;\n"
                        + "import cn.hutool.crypto.SecureUtil;\n"
                        + "import cn.hutool.crypto.asymmetric.SM2;\n"
                        + "import java.util.TreeMap;\n"
                        + "import java.util.Map;\n\n"
                        + "public class Sm2SignDemo {\n"
                        + "    public static String sign(Map<String, Object> params, String privateKeyHex) {\n"
                        + "        TreeMap<String, Object> sortedMap = new TreeMap<>(params);\n"
                        + "        StringBuilder sb = new StringBuilder();\n"
                        + "        for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {\n"
                        + "            if (entry.getValue() != null && !\"sign\".equals(entry.getKey()) && !\"signType\".equals(entry.getKey())) {\n"
                        + "                sb.append(entry.getKey()).append(\"=\").append(entry.getValue()).append(\"&\");\n"
                        + "            }\n"
                        + "        }\n"
                        + "        sb.deleteCharAt(sb.length() - 1);\n"
                        + "        SM2 sm2 = SecureUtil.sm2(privateKeyHex, null);\n"
                        + "        byte[] signBytes = sm2.sign(sb.toString().getBytes());\n"
                        + "        return HexUtil.encodeHexStr(signBytes);\n"
                        + "    }\n"
                        + "}";
            default:
                return "import java.security.MessageDigest;\n"
                        + "import java.util.TreeMap;\n"
                        + "import java.util.Map;\n\n"
                        + "public class Md5SignDemo {\n"
                        + "    public static String sign(Map<String, Object> params, String md5Key) throws Exception {\n"
                        + "        TreeMap<String, Object> sortedMap = new TreeMap<>(params);\n"
                        + "        StringBuilder sb = new StringBuilder();\n"
                        + "        for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {\n"
                        + "            if (entry.getValue() != null && !\"sign\".equals(entry.getKey()) && !\"signType\".equals(entry.getKey())) {\n"
                        + "                sb.append(entry.getKey()).append(\"=\").append(entry.getValue()).append(\"&\");\n"
                        + "            }\n"
                        + "        }\n"
                        + "        sb.append(\"key=\").append(md5Key);\n"
                        + "        MessageDigest md = MessageDigest.getInstance(\"MD5\");\n"
                        + "        byte[] digest = md.digest(sb.toString().getBytes(\"UTF-8\"));\n"
                        + "        StringBuilder hexString = new StringBuilder();\n"
                        + "        for (byte b : digest) {\n"
                        + "            String hex = Integer.toHexString(0xff & b);\n"
                        + "            if (hex.length() == 1) hexString.append('0');\n"
                        + "            hexString.append(hex);\n"
                        + "        }\n"
                        + "        return hexString.toString().toUpperCase();\n"
                        + "    }\n"
                        + "}";
        }
    }

    private String generatePhpCode(String signType, Map<String, Object> params, String key) {
        switch (signType) {
            case "RSA":
                return "<?php\n"
                        + "function rsaSign($params, $privateKeyPem) {\n"
                        + "    ksort($params);\n"
                        + "    $signStr = '';\n"
                        + "    foreach ($params as $k => $v) {\n"
                        + "        if ($v !== '' && $v !== null && $k !== 'sign' && $k !== 'signType') {\n"
                        + "            $signStr .= $k . '=' . $v . '&';\n"
                        + "        }\n"
                        + "    }\n"
                        + "    $signStr = rtrim($signStr, '&');\n"
                        + "    openssl_sign($signStr, $signature, $privateKeyPem, OPENSSL_ALGO_SHA256);\n"
                        + "    return base64_encode($signature);\n"
                        + "}\n";
            case "SM2":
                return "<?php\n"
                        + "function sm2Sign($params, $privateKeyHex) {\n"
                        + "    ksort($params);\n"
                        + "    $signStr = '';\n"
                        + "    foreach ($params as $k => $v) {\n"
                        + "        if ($v !== '' && $v !== null && $k !== 'sign' && $k !== 'signType') {\n"
                        + "            $signStr .= $k . '=' . $v . '&';\n"
                        + "        }\n"
                        + "    }\n"
                        + "    $signStr = rtrim($signStr, '&');\n"
                        + "    // 需要安装SM2扩展或使用openssl SM2支持\n"
                        + "    $signature = '';\n"
                        + "    openssl_sign($signStr, $signature, $privateKeyHex, 'sm2');\n"
                        + "    return bin2hex($signature);\n"
                        + "}\n";
            default:
                return "<?php\n"
                        + "function md5Sign($params, $md5Key) {\n"
                        + "    ksort($params);\n"
                        + "    $signStr = '';\n"
                        + "    foreach ($params as $k => $v) {\n"
                        + "        if ($v !== '' && $v !== null && $k !== 'sign' && $k !== 'signType') {\n"
                        + "            $signStr .= $k . '=' . $v . '&';\n"
                        + "        }\n"
                        + "    }\n"
                        + "    $signStr .= 'key=' . $md5Key;\n"
                        + "    return strtoupper(md5($signStr));\n"
                        + "}\n";
        }
    }

    private String generatePythonCode(String signType, Map<String, Object> params, String key) {
        switch (signType) {
            case "RSA":
                return "from Crypto.Signature import pkcs1_15\n"
                        + "from Crypto.Hash import SHA256\n"
                        + "from Crypto.PublicKey import RSA\n"
                        + "import base64\n\n"
                        + "def rsa_sign(params, private_key_pem):\n"
                        + "    sorted_params = sorted(params.items())\n"
                        + "    sign_str = '&'.join(f'{k}={v}' for k, v in sorted_params if v is not None and v != '' and k not in ('sign', 'signType'))\n"
                        + "    key = RSA.import_key(private_key_pem)\n"
                        + "    h = SHA256.new(sign_str.encode('utf-8'))\n"
                        + "    signature = pkcs1_15.new(key).sign(h)\n"
                        + "    return base64.b64encode(signature).decode('utf-8')\n";
            case "SM2":
                return "from gmssl import sm2\n\n"
                        + "def sm2_sign(params, private_key_hex):\n"
                        + "    sorted_params = sorted(params.items())\n"
                        + "    sign_str = '&'.join(f'{k}={v}' for k, v in sorted_params if v is not None and v != '' and k not in ('sign', 'signType'))\n"
                        + "    sm2_crypto = sm2.CryptSM2(private_key=private_key_hex, public_key='')\n"
                        + "    sign_bytes = sm2_crypto.sign(sign_str.encode('utf-8'), private_key_hex)\n"
                        + "    return sign_bytes.hex() if isinstance(sign_bytes, bytes) else sign_bytes\n";
            default:
                return "import hashlib\n\n"
                        + "def md5_sign(params, md5_key):\n"
                        + "    sorted_params = sorted(params.items())\n"
                        + "    sign_str = '&'.join(f'{k}={v}' for k, v in sorted_params if v is not None and v != '' and k not in ('sign', 'signType'))\n"
                        + "    sign_str += '&key=' + md5_key\n"
                        + "    return hashlib.md5(sign_str.encode('utf-8')).hexdigest().upper()\n";
        }
    }

    private String formatParamsForCode(Map<String, Object> params) {
        TreeMap<String, Object> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            if (entry.getValue() != null && !"sign".equals(entry.getKey()) && !"signType".equals(entry.getKey())) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private CallbackSimulateVO convertToVO(CallbackSimulateLog logEntity) {
        CallbackSimulateVO vo = BeanUtil.copyProperties(logEntity, CallbackSimulateVO.class);
        vo.setCallbackStatusDesc(getCallbackStatusDesc(logEntity.getCallbackStatus()));
        if (logEntity.getCreatedAt() != null) {
            vo.setCreatedAt(logEntity.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return vo;
    }

    private String getCallbackStatusDesc(Integer callbackStatus) {
        if (callbackStatus == null) {
            return "";
        }
        switch (callbackStatus) {
            case 0:
                return "待发送";
            case 1:
                return "发送成功";
            case 2:
                return "发送失败";
            case 3:
                return "已重发";
            default:
                return "";
        }
    }
}
