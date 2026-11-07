package com.jobboard.api.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimiter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public boolean tryConsume(String key, int limit, Duration window) {
        Instant now = Instant.now();
        Window current = windows.compute(key, (k, existing) ->
                existing == null || !existing.resetAt().isAfter(now)
                        ? new Window(now.plus(window), new AtomicInteger())
                        : existing);

        if (current.count().incrementAndGet() > limit) {
            return false;
        }

        evictExpired(now);
        return true;
    }

    private void evictExpired(Instant now) {
        if (ThreadLocalRandom.current().nextInt(100) != 0) {
            return;
        }
        Iterator<Map.Entry<String, Window>> iterator = windows.entrySet().iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().getValue().resetAt().isAfter(now)) {
                iterator.remove();
            }
        }
    }

    private record Window(Instant resetAt, AtomicInteger count) {
    }
}
