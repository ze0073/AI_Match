package com.aimatch.config.ratelimit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    double permitsPerSecond() default 5.0 / 60.0;
    long timeoutSeconds() default 10;
}
