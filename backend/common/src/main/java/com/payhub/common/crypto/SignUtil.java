package com.payhub.common.crypto;

import com.payhub.common.enums.SignTypeEnum;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class SignUtil {

    private SignUtil() {
    }

    public static String generateSign(Map<String, Object> params, String signType, String key) {
        SignTypeEnum signTypeEnum = SignTypeEnum.getByCode(signType);
        if (signTypeEnum == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的签名类型: " + signType);
        }
        String signContent = buildSignContent(params);
        return switch (signTypeEnum) {
            case MD5 -> Md5Util.md5(signContent + key).toUpperCase();
            case RSA -> RsaUtil.sign(signContent, key);
            case SM2 -> SM2Util.sign(signContent, key);
        };
    }

    public static boolean verifySign(Map<String, Object> params, String signType, String sign, String key) {
        try {
            SignTypeEnum signTypeEnum = SignTypeEnum.getByCode(signType);
            if (signTypeEnum == null) {
                log.warn("不支持的签名类型: {}", signType);
                return false;
            }
            String signContent = buildSignContent(params);
            return switch (signTypeEnum) {
                case MD5 -> Md5Util.md5(signContent + key).toUpperCase().equalsIgnoreCase(sign);
                case RSA -> RsaUtil.verify(signContent, sign, key);
                case SM2 -> SM2Util.verify(signContent, sign, key);
            };
        } catch (Exception e) {
            log.error("验证签名异常", e);
            return false;
        }
    }

    public static String buildSignContent(Map<String, Object> params) {
        TreeMap<String, Object> sortedParams = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : sortedParams.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null || "".equals(value.toString().trim()) || "sign".equals(key) || "signType".equals(key)) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append("&");
            }
            sb.append(key).append("=").append(value);
        }
        return sb.toString();
    }

    public static boolean checkTimestamp(Long timestamp, int expireSeconds) {
        if (timestamp == null) {
            return false;
        }
        long diff = System.currentTimeMillis() - timestamp;
        return diff >= 0 && diff <= expireSeconds * 1000L;
    }

    public static boolean checkNonce(String nonce, String merchantNo, int expireSeconds) {
        if (nonce == null || nonce.isEmpty()) {
            return false;
        }
        return true;
    }
}
