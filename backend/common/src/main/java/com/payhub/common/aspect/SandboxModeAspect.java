package com.payhub.common.aspect;

import com.payhub.common.annotation.SandboxMode;
import com.payhub.common.context.SandboxContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@Order(1)
public class SandboxModeAspect {

    @Around("@within(sandboxMode) || @annotation(sandboxMode)")
    public Object around(ProceedingJoinPoint joinPoint, SandboxMode sandboxMode) throws Throwable {
        if (sandboxMode == null) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            sandboxMode = signature.getMethod().getAnnotation(SandboxMode.class);
            if (sandboxMode == null) {
                sandboxMode = joinPoint.getTarget().getClass().getAnnotation(SandboxMode.class);
            }
        }

        boolean originalSandbox = SandboxContext.isSandboxMode();
        try {
            if (sandboxMode != null) {
                SandboxContext.setSandboxMode(sandboxMode.value());
                log.debug("切换沙箱模式: {}", sandboxMode.value());
            }
            return joinPoint.proceed();
        } finally {
            SandboxContext.setSandboxMode(originalSandbox);
        }
    }
}
