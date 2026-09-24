package com.healthplatform.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Per-client-IP fixed-window rate limit on /api/auth/* (login, register, refresh, MFA). Complements the
 * per-account lockout in LoginRateLimiter: that stops guessing one account, this stops one client
 * spraying many accounts or hammering register/refresh. Redis-backed so the limit holds across replicas.
 * Fails OPEN if Redis is unreachable (logged) — availability of login beats a limiter outage.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redis;
    private final int maxPerWindow;

    public AuthRateLimitFilter(StringRedisTemplate redis,
                               @Value("${app.rate-limit.auth-requests-per-minute:30}") int maxPerWindow) {
        this.redis = redis;
        this.maxPerWindow = maxPerWindow;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/auth/") || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {
        if (isOverLimit(request.getRemoteAddr())) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many requests. Try again shortly.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isOverLimit(String clientIp) {
        try {
            String key = "auth-rate:" + clientIp;
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, WINDOW);
            }
            return count != null && count > maxPerWindow;
        } catch (RuntimeException ex) {
            log.warn("Auth rate limiter unavailable, allowing request: {}", ex.toString());
            return false;
        }
    }
}
