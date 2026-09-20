package com.healthplatform.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed brute-force protection on /auth/login — see docs/THREAT_MODEL.md #9.
 * Counts failures per account; does not lock out on successful logins.
 */
@Service
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    public LoginRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isLocked(String email) {
        String value = redisTemplate.opsForValue().get(key(email));
        return value != null && Integer.parseInt(value) >= MAX_ATTEMPTS;
    }

    public void recordFailure(String email) {
        String k = key(email);
        Long count = redisTemplate.opsForValue().increment(k);
        if (count != null && count == 1L) {
            redisTemplate.expire(k, WINDOW);
        }
    }

    public void reset(String email) {
        redisTemplate.delete(key(email));
    }

    private String key(String email) {
        return "login-attempts:" + email.toLowerCase();
    }
}
