package com.payhub.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiLog {

    String value() default "";

    String module() default "";

    boolean recordParams() default true;

    boolean recordResult() default true;

    int maxResultLength() default 2000;
}
