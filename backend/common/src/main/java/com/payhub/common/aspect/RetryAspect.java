package com.payhub.common.aspect;

import com.payhub.common.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@Order(2)
public class RetryAspect {

    @Around("@annotation(retry)")
    public Object around(ProceedingJoinPoint joinPoint, Retry retry) throws Throwable {
        int maxAttempts = retry.maxAttempts();
        long currentDelay = retry.initialDelay();
        double multiplier = retry.multiplier();
        long maxDelay = retry.maxDelay();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        Class<? extends Throwable>[] retryFor = retry.retryFor();
        Class<? extends Throwable>[] noRetryFor = retry.noRetryFor();

        Throwable lastThrowable = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Object result = joinPoint.proceed();
                if (attempt > 1) {
                    log.info("[重试成功] method: {}, attempt: {}/{}", methodName, attempt, maxAttempts);
                }
                return result;
            } catch (Throwable t) {
                lastThrowable = t;

                if (!shouldRetry(t, retryFor, noRetryFor)) {
                    log.warn("[不重试] method: {}, exception: {}", methodName, t.getClass().getName());
                    throw t;
                }

                if (attempt >= maxAttempts) {
                    log.error("[重试耗尽] method: {}, attempts: {}, error: {}",
                            methodName, maxAttempts, t.getMessage(), t);
                    throw t;
                }

                log.warn("[重试中] method: {}, attempt: {}/{}, delay: {}ms, error: {}",
                        methodName, attempt, maxAttempts, currentDelay, t.getMessage());

                try {
                    Thread.sleep(currentDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw t;
                }

                currentDelay = Math.min((long) (currentDelay * multiplier), maxDelay);
            }
        }

        throw lastThrowable != null ? lastThrowable : new RuntimeException("Retry failed");
    }

    private boolean shouldRetry(Throwable t, Class<? extends Throwable>[] retryFor,
                                Class<? extends Throwable>[] noRetryFor) {
        for (Class<? extends Throwable> noRetryClass : noRetryFor) {
            if (noRetryClass.isInstance(t)) {
                return false;
            }
        }

        if (retryFor.length == 0 || (retryFor.length == 1 && retryFor[0] == Exception.class)) {
            return true;
        }

        for (Class<? extends Throwable> retryClass : retryFor) {
            if (retryClass.isInstance(t)) {
                return true;
            }
        }

        return false;
    }
}
