package com.aimatch.config.ratelimit;

import com.aimatch.model.Result;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String key = "global";
        if (attributes != null) {
            String ip = attributes.getRequest().getRemoteAddr();
            key = joinPoint.getSignature().toShortString() + "_" + ip;
        }

        RateLimiter limiter = limiters.computeIfAbsent(key,
                k -> RateLimiter.create(rateLimit.permitsPerSecond()));

        if (!limiter.tryAcquire(rateLimit.timeoutSeconds(), TimeUnit.SECONDS)) {
            log.warn("限流触发: {}", key);
            return Result.error(429, "请求过于频繁，请稍后再试");
        }

        return joinPoint.proceed();
    }
}
