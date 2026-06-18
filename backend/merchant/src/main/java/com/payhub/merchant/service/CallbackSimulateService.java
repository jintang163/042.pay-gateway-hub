package com.payhub.merchant.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.HttpUtil;
import com.payhub.common.utils.SignUtil;
import com.payhub.merchant.dto.CallbackSimulateRequest;
import com.payhub.merchant.dto.CallbackSimulateVO;
import com.payhub.merchant.dto.SignCodeExampleRequest;
import com.payhub.merchant.dto.SignCodeExampleVO;
import com.payhub.merchant.entity.CallbackSimulateLog;
import com.payhub.merchant.mapper.CallbackSimulateLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
public class CallbackSimulateService {

    @Autowired
    private CallbackSimulateLogMapper callbackSimulateLogMapper;

    public CallbackSimulateVO simulate(CallbackSimulateRequest request) {
        String logNo = "CS" + System.currentTimeMillis() + RandomUtil.randomNumbers(4);
        String orderNo = StrUtil.isNotBlank(request.getOrderNo())
                ? request.getOrderNo()
                : "PG" + System.currentTimeMillis();

        TreeMap<String, Object> params = new TreeMap<>();
        params.put("merchantNo", request.getMerchantNo());
        params.put("orderNo", orderNo);
        params.put("callbackType", request.getCallbackType());
        params.put("status", request.getSimulateStatus());
        if (request.getAmount() != null) {
            params.put("amount", request.getAmount());
        }
        params.put("timestamp", System.currentTimeMillis());

        String signType = StrUtil.isNotBlank(request.getSignType()) ? request.getSignType().toUpperCase() : "MD5";
        String md5Key = null;
        String rsaPrivateKey = null;
        String sm2PrivateKey = null;

        switch (signType) {
            case "RSA":
                SignUtil.RsaKeyPair rsaKeyPair = SignUtil.generateRsaKeyPair();
                rsaPrivateKey = rsaKeyPair.getPrivateKey();
                break;
            case "SM2":
                SignUtil.Sm2KeyPair sm2KeyPair = SignUtil.generateSm2KeyPair();
                sm2PrivateKey = sm2KeyPair.getPrivateKey();
                break;
            default:
                md5Key = "test_md5_key_123456";
                break;
        }

        String sign = SignUtil.sign(params, signType, md5Key, rsaPrivateKey, sm2PrivateKey);
        params.put("sign", sign);
        params.put("signType", signType);

        String requestBody = JSON.toJSONString(params);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("X-Callback-Simulate", "true");

        String callbackUrl = request.getCallbackUrl();
        long startTime = System.currentTimeMillis();
        String responseBody = null;
        Integer responseHttpStatus = null;
        try {
            responseBody = HttpUtil.postJson(callbackUrl, params, headers);
            responseHttpStatus = 200;
        } catch (Exception e) {
            log.error("回调模拟发送失败: logNo={}, url={}", logNo, callbackUrl, e);
            responseHttpStatus = 500;
            responseBody = e.getMessage();
        }
        long responseTimeMs = System.currentTimeMillis() - startTime;

        CallbackSimulateLog logEntity = new CallbackSimulateLog();
        logEntity.setLogNo(logNo);
        logEntity.setMerchantNo(request.getMerchantNo());
        logEntity.setOrderNo(orderNo);
        logEntity.setCallbackUrl(callbackUrl);
        logEntity.setCallbackType(request.getCallbackType());
        logEntity.setSimulateStatus(request.getSimulateStatus());
        logEntity.setSignType(signType);
        logEntity.setRequestHeaders(JSON.toJSONString(headers));
        logEntity.setRequestBody(requestBody);
        logEntity.setResponseHttpStatus(responseHttpStatus);
        logEntity.setResponseBody(responseBody);
        logEntity.setResponseTimeMs((int) responseTimeMs);
        logEntity.setCallbackStatus(responseHttpStatus == 200 && StrUtil.isNotBlank(responseBody) ? 1 : 2);
        logEntity.setRetryCount(0);
        logEntity.setRemark(request.getRemark());
        callbackSimulateLogMapper.insert(logEntity);

        log.info("回调模拟完成: logNo={}, callbackStatus={}, responseTimeMs={}", logNo, logEntity.getCallbackStatus(), responseTimeMs);

        return convertToVO(logEntity);
    }

    public IPage<CallbackSimulateVO> listPage(Long current, Long size, Map<String, Object> params) {
        Page<CallbackSimulateLog> page = new Page<>(current, size);
        LambdaQueryWrapper<CallbackSimulateLog> wrapper = new LambdaQueryWrapper<>();

        if (params != null) {
            String merchantNo = (String) params.get("merchantNo");
            if (StringUtils.hasText(merchantNo)) {
                wrapper.eq(CallbackSimulateLog::getMerchantNo, merchantNo);
            }
            String callbackType = (String) params.get("callbackType");
            if (StringUtils.hasText(callbackType)) {
                wrapper.eq(CallbackSimulateLog::getCallbackType, callbackType);
            }
            String simulateStatus = (String) params.get("simulateStatus");
            if (StringUtils.hasText(simulateStatus)) {
                wrapper.eq(CallbackSimulateLog::getSimulateStatus, simulateStatus);
            }
            String callbackStatus = (String) params.get("callbackStatus");
            if (StringUtils.hasText(callbackStatus)) {
                wrapper.eq(CallbackSimulateLog::getCallbackStatus, Integer.valueOf(callbackStatus));
            }
        }

        wrapper.orderByDesc(CallbackSimulateLog::getCreatedAt);

        IPage<CallbackSimulateLog> logPage = callbackSimulateLogMapper.selectPage(page, wrapper);

        Page<CallbackSimulateVO> voPage = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        voPage.setRecords(logPage.getRecords().stream().map(this::convertToVO).collect(java.util.stream.Collectors.toList()));

        return voPage;
    }

    public CallbackSimulateVO resend(String logNo) {
        LambdaQueryWrapper<CallbackSimulateLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CallbackSimulateLog::getLogNo, logNo).last("LIMIT 1");
        CallbackSimulateLog logEntity = callbackSimulateLogMapper.selectOne(wrapper);
        if (logEntity == null) {
            throw new BusinessException(ResultCode.CALLBACK_TEST_FAIL, "回调模拟记录不存在");
        }

        JSONObject requestBody = JSON.parseObject(logEntity.getRequestBody());
        Map<String, Object> params = new HashMap<>(requestBody);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("X-Callback-Simulate", "true");

        long startTime = System.currentTimeMillis();
        String responseBody = null;
        Integer responseHttpStatus = null;
        try {
            responseBody = HttpUtil.postJson(logEntity.getCallbackUrl(), params, headers);
            responseHttpStatus = 200;
        } catch (Exception e) {
            log.error("回调重发失败: logNo={}, url={}", logNo, logEntity.getCallbackUrl(), e);
            responseHttpStatus = 500;
            responseBody = e.getMessage();
        }
        long responseTimeMs = System.currentTimeMillis() - startTime;

        logEntity.setRetryCount(logEntity.getRetryCount() + 1);
        logEntity.setResponseHttpStatus(responseHttpStatus);
        logEntity.setResponseBody(responseBody);
        logEntity.setResponseTimeMs((int) responseTimeMs);
        logEntity.setCallbackStatus(3);
        callbackSimulateLogMapper.updateById(logEntity);

        log.info("回调重发完成: logNo={}, retryCount={}, responseTimeMs={}", logNo, logEntity.getRetryCount(), responseTimeMs);

        return convertToVO(logEntity);
    }

    public SignCodeExampleVO generateSignCode(SignCodeExampleRequest request) {
        String language = request.getLanguage().toUpperCase();
        String signType = request.getSignType().toUpperCase();

        String code = generateCode(language, signType, request.getParams(), request.getKey());
        String description = language + " " + signType + " 签名示例";

        SignCodeExampleVO vo = new SignCodeExampleVO();
        vo.setLanguage(language);
        vo.setSignType(signType);
        vo.setCode(code);
        vo.setDescription(description);
        return vo;
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
            vo.setCreatedAt(logEntity.getCreatedAt().toString());
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
