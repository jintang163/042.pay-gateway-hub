package com.payhub.common.interceptor;

import com.payhub.common.utils.JsonUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "apiStartTime";
    private static final String TRACE_ID_ATTR = "apiTraceId";
    private static final String LOG_KEY_PREFIX = "payhub:api:log:";
    private static final int LOG_EXPIRE_HOURS = 24;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString().replace("-", "");

        request.setAttribute(START_TIME_ATTR, startTime);
        request.setAttribute(TRACE_ID_ATTR, traceId);

        log.info("[API请求开始] traceId: {}, method: {}, uri: {}, ip: {}, userAgent: {}",
                traceId,
                request.getMethod(),
                request.getRequestURI(),
                getClientIp(request),
                request.getHeader("User-Agent"));

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        String traceId = (String) request.getAttribute(TRACE_ID_ATTR);
        long costTime = startTime == null ? 0L : System.currentTimeMillis() - startTime;

        Map<String, Object> logMap = new HashMap<>();
        logMap.put("traceId", traceId);
        logMap.put("method", request.getMethod());
        logMap.put("uri", request.getRequestURI());
        logMap.put("queryString", request.getQueryString());
        logMap.put("ip", getClientIp(request));
        logMap.put("userAgent", request.getHeader("User-Agent"));
        logMap.put("merchantNo", request.getAttribute("currentMerchantNo"));
        logMap.put("status", response.getStatus());
        logMap.put("costTime", costTime);
        logMap.put("createTime", System.currentTimeMillis());

        if (ex != null) {
            logMap.put("error", ex.getMessage());
        }

        try {
            String logKey = LOG_KEY_PREFIX + traceId;
            stringRedisTemplate.opsForValue().set(logKey, JsonUtils.toJson(logMap), Duration.ofHours(LOG_EXPIRE_HOURS));
        } catch (Exception e) {
            log.error("保存API日志到Redis失败", e);
        }

        if (ex != null) {
            log.error("[API请求异常] traceId: {}, method: {}, uri: {}, cost: {}ms, error: {}",
                    traceId, request.getMethod(), request.getRequestURI(), costTime, ex.getMessage(), ex);
        } else {
            log.info("[API请求结束] traceId: {}, method: {}, uri: {}, status: {}, cost: {}ms",
                    traceId, request.getMethod(), request.getRequestURI(), response.getStatus(), costTime);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
