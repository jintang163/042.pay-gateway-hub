package com.payhub.common.interceptor;

import com.payhub.common.context.SandboxContext;
import com.payhub.common.crypto.SignKeyProvider;
import com.payhub.common.crypto.SignUtil;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignInterceptor implements HandlerInterceptor {

    private static final String HEADER_MERCHANT_NO = "X-Merchant-No";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String HEADER_SIGN = "X-Sign";
    private static final String HEADER_SIGN_TYPE = "X-Sign-Type";
    private static final int DEFAULT_EXPIRE_SECONDS = 300;
    private static final String NONCE_KEY_PREFIX = "payhub:nonce:";
    private static final String SANDBOX_SIGN_KEY = "sandbox_test_sign_key_2024";

    private final StringRedisTemplate stringRedisTemplate;
    private final SignKeyProvider signKeyProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String merchantNo = request.getHeader(HEADER_MERCHANT_NO);
        String timestampStr = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);
        String sign = request.getHeader(HEADER_SIGN);
        String signType = request.getHeader(HEADER_SIGN_TYPE);

        if (StringUtils.isAnyBlank(merchantNo, timestampStr, nonce, sign, signType)) {
            log.warn("签名参数缺失, merchantNo: {}, timestamp: {}, nonce: {}, sign: {}, signType: {}",
                    merchantNo, timestampStr, nonce, sign, signType);
            throw new BusinessException(ResultCode.SIGN_PARAM_MISSING);
        }

        Long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            log.warn("时间戳格式错误: {}", timestampStr);
            throw new BusinessException(ResultCode.PARAM_ERROR, "时间戳格式错误");
        }

        if (!SandboxContext.isSandboxMode()) {
            if (!SignUtil.checkTimestamp(timestamp, DEFAULT_EXPIRE_SECONDS)) {
                log.warn("请求已过期, timestamp: {}, merchantNo: {}", timestamp, merchantNo);
                throw new BusinessException(ResultCode.SIGN_TIMEOUT);
            }

            String nonceKey = NONCE_KEY_PREFIX + merchantNo + ":" + nonce;
            Boolean nonceExists = stringRedisTemplate.hasKey(nonceKey);
            if (Boolean.TRUE.equals(nonceExists)) {
                log.warn("重复请求, nonce: {}, merchantNo: {}", nonce, merchantNo);
                throw new BusinessException(ResultCode.SIGN_NONCE_REPEAT);
            }
        } else {
            if (!SignUtil.checkTimestamp(timestamp, DEFAULT_EXPIRE_SECONDS * 12)) {
                log.warn("沙箱请求已过期, timestamp: {}, merchantNo: {}", timestamp, merchantNo);
                throw new BusinessException(ResultCode.SIGN_TIMEOUT);
            }
            log.info("沙箱模式签名校验, merchantNo: {}, 跳过nonce去重检查", merchantNo);
        }

        Map<String, Object> signParams = buildSignParams(request, merchantNo, timestamp, nonce, signType);
        String signKey = getSignKey(merchantNo, signType);

        if (!SignUtil.verifySign(signParams, signType, sign, signKey)) {
            log.warn("签名验证失败, merchantNo: {}, signType: {}, params: {}", merchantNo, signType, signParams);
            throw new BusinessException(ResultCode.SIGN_ERROR);
        }

        if (!SandboxContext.isSandboxMode()) {
            stringRedisTemplate.opsForValue().set(NONCE_KEY_PREFIX + merchantNo + ":" + nonce, "1", Duration.ofSeconds(DEFAULT_EXPIRE_SECONDS));
        }

        request.setAttribute("currentMerchantNo", merchantNo);
        return true;
    }

    private String getSignKey(String merchantNo, String signType) {
        if (SandboxContext.isSandboxMode()) {
            log.info("沙箱模式使用测试签名密钥, merchantNo: {}", merchantNo);
            return SANDBOX_SIGN_KEY;
        }
        return signKeyProvider.getSignKey(merchantNo, signType);
    }

    private Map<String, Object> buildSignParams(HttpServletRequest request, String merchantNo, Long timestamp,
                                                String nonce, String signType) {
        Map<String, Object> params = new HashMap<>();

        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String[] values = entry.getValue();
            if (values != null && values.length > 0) {
                params.put(entry.getKey(), values.length == 1 ? values[0] : values);
            }
        }

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (headerName.startsWith("X-") && !HEADER_SIGN.equalsIgnoreCase(headerName)) {
                params.put(headerName, request.getHeader(headerName));
            }
        }

        params.put(HEADER_MERCHANT_NO, merchantNo);
        params.put(HEADER_TIMESTAMP, timestamp);
        params.put(HEADER_NONCE, nonce);
        params.put(HEADER_SIGN_TYPE, signType);

        return params;
    }
}
