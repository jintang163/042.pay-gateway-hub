package com.payhub.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Retry {

    int maxAttempts() default 3;

    long initialDelay() default 1000L;

    double multiplier() default 2.0;

    long maxDelay() default 30000L;

    Class<? extends Throwable>[] retryFor() default {Exception.class};

    Class<? extends Throwable>[] noRetryFor() default {};
}
