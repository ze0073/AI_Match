package com.aimatch.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MatchCacheManager {

    private static final long TTL_MS = 30 * 60 * 1000;

    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        CacheEntry<?> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            log.debug("缓存命中: {}", key);
            return (T) entry.value;
        }
        if (entry != null) {
            cache.remove(key);
        }
        return null;
    }

    public <T> void put(String key, T value) {
        cache.put(key, new CacheEntry<>(value, System.currentTimeMillis() + TTL_MS));
        log.debug("缓存写入: {}", key);
    }

    public void clear() {
        cache.clear();
    }

    private record CacheEntry<T>(T value, long expireTime) {
        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    public String buildMatchKey(Long resumeId, Long jobId) {
        return "match:" + resumeId + ":" + jobId;
    }
}
