package com.example.concurrency.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    String key();               // 락 키

    long waitTime() default 5;  // 락 대기 시간

    long leaseTime() default 3; // 락 점유 시간

    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
