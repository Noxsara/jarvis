package com.avengers.jarvis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Monitor {

    String bizType() default "";

    long expectedExecuteTime() default 5000L;

    MonitorExtend[] monitorExtend() default {};

    boolean ignoreException() default false;
}