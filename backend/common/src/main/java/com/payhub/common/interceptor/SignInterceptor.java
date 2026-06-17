package com.payhub.common.interceptor;

import com.payhub.common.crypto.SignUtil;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultEnum;
import com.payhub.common.utils.JsonUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.BufferedReader;
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

    private final StringRedisTemplate stringRedisTemplate;

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
            throw new BusinessException(ResultEnum.SIGN_PARAM_MISSING);
        }

        Long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            log.warn("时间戳格式错误: {}", timestampStr);
            throw new BusinessException(ResultEnum.PARAM_ERROR, "时间戳格式错误");
        }

        if (!SignUtil.checkTimestamp(timestamp, DEFAULT_EXPIRE_SECONDS)) {
            log.warn("请求已过期, timestamp: {}, merchantNo: {}", timestamp, merchantNo);
            throw new BusinessException(ResultEnum.SIGN_TIMEOUT);
        }

        String nonceKey = NONCE_KEY_PREFIX + merchantNo + ":" + nonce;
        Boolean nonceExists = stringRedisTemplate.hasKey(nonceKey);
        if (Boolean.TRUE.equals(nonceExists)) {
            log.warn("重复请求, nonce: {}, merchantNo: {}", nonce, merchantNo);
            throw new BusinessException(ResultEnum.SIGN_ERROR, "重复请求");
        }

        Map<String, Object> signParams = buildSignParams(request, merchantNo, timestamp, nonce, signType);
        String signKey = getMerchantSignKey(merchantNo);

        if (!SignUtil.verifySign(signParams, signType, sign, signKey)) {
            log.warn("签名验证失败, merchantNo: {}, signType: {}, params: {}", merchantNo, signType, signParams);
            throw new BusinessException(ResultEnum.SIGN_ERROR);
        }

        stringRedisTemplate.opsForValue().set(nonceKey, "1", Duration.ofSeconds(DEFAULT_EXPIRE_SECONDS));

        request.setAttribute("currentMerchantNo", merchantNo);
        return true;
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

        String body = getRequestBody(request);
        if (StringUtils.isNotBlank(body)) {
            try {
                Map<String, Object> bodyMap = JsonUtils.parseMap(body);
                if (bodyMap != null) {
                    params.putAll(bodyMap);
                }
            } catch (Exception e) {
                log.warn("解析请求体失败: {}", body);
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

    private String getRequestBody(HttpServletRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("获取请求体失败", e);
            return null;
        }
    }

    protected String getMerchantSignKey(String merchantNo) {
        return "default_sign_key_please_override";
    }
}
