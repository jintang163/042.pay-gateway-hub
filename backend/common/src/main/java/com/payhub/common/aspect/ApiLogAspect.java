package com.payhub.common.aspect;

import com.payhub.common.annotation.ApiLog;
import com.payhub.common.utils.JsonUtils;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class ApiLogAspect {

    @Around("@annotation(apiLog)")
    public Object around(ProceedingJoinPoint joinPoint, ApiLog apiLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString().replace("-", "");
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String operation = apiLog.value().isEmpty() ? methodName : apiLog.value();
        String module = apiLog.module();

        HttpServletRequest request = getRequest();
        String ip = request != null ? getClientIp(request) : "unknown";
        String uri = request != null ? request.getRequestURI() : "";
        String merchantNo = request != null ? (String) request.getAttribute("currentMerchantNo") : null;

        if (apiLog.recordParams()) {
            Map<String, Object> params = getParams(joinPoint, signature);
            log.info("[API日志开始] traceId: {}, module: {}, operation: {}, class: {}, method: {}, uri: {}, ip: {}, merchantNo: {}, params: {}",
                    traceId, module, operation, className, methodName, uri, ip, merchantNo, truncate(JsonUtils.toJson(params), 2000));
        } else {
            log.info("[API日志开始] traceId: {}, module: {}, operation: {}, class: {}, method: {}, uri: {}, ip: {}, merchantNo: {}",
                    traceId, module, operation, className, methodName, uri, ip, merchantNo);
        }

        Object result = null;
        Throwable throwable = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            throwable = t;
            throw t;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            if (throwable != null) {
                log.error("[API日志异常] traceId: {}, module: {}, operation: {}, cost: {}ms, error: {}",
                        traceId, module, operation, costTime, throwable.getMessage(), throwable);
            } else {
                if (apiLog.recordResult()) {
                    String resultStr = truncate(JsonUtils.toJson(result), apiLog.maxResultLength());
                    log.info("[API日志结束] traceId: {}, module: {}, operation: {}, cost: {}ms, result: {}",
                            traceId, module, operation, costTime, resultStr);
                } else {
                    log.info("[API日志结束] traceId: {}, module: {}, operation: {}, cost: {}ms",
                            traceId, module, operation, costTime);
                }
            }
        }
    }

    private Map<String, Object> getParams(ProceedingJoinPoint joinPoint, MethodSignature signature) {
        Map<String, Object> params = new HashMap<>();
        String[] paramNames = signature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();
        for (int i = 0; i < paramNames.length; i++) {
            Object value = paramValues[i];
            if (value instanceof HttpServletRequest) {
                continue;
            }
            params.put(paramNames[i], value);
        }
        return params;
    }

    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
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

    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...(truncated)";
    }
}
