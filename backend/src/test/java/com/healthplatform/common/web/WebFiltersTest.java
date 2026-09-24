package com.healthplatform.common.web;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class WebFiltersTest {

    // ---- RequestIdFilter ----

    @Test
    void requestId_generatedWhenAbsentAndEchoedInResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenInMdc = new AtomicReference<>();

        new RequestIdFilter().doFilter(new MockHttpServletRequest(), response,
                (req, res) -> seenInMdc.set(MDC.get(RequestIdFilter.MDC_KEY)));

        assertNotNull(response.getHeader(RequestIdFilter.HEADER));
        assertEquals(response.getHeader(RequestIdFilter.HEADER), seenInMdc.get());
        assertNull(MDC.get(RequestIdFilter.MDC_KEY), "MDC must be cleaned up after the request");
    }

    @Test
    void requestId_reusesWellFormedInboundId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "trace-abc12345");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RequestIdFilter().doFilter(request, response, new MockFilterChain());

        assertEquals("trace-abc12345", response.getHeader(RequestIdFilter.HEADER));
    }

    @Test
    void requestId_replacesMaliciousInboundId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "x\r\nSet-Cookie: pwned");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RequestIdFilter().doFilter(request, response, new MockFilterChain());

        assertFalse(response.getHeader(RequestIdFilter.HEADER).contains("pwned"));
    }

    // ---- AuthRateLimitFilter ----

    private static StringRedisTemplate redisReturning(Long count) {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(count);
        return redis;
    }

    @Test
    void rateLimit_allowsRequestsUnderTheLimit() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(redisReturning(3L), 5);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("POST", "/api/auth/login"), response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    void rateLimit_blocksRequestsOverTheLimitWith429() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(redisReturning(6L), 5);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("POST", "/api/auth/login"), response, chain);

        assertEquals(429, response.getStatus());
        assertNotNull(response.getHeader("Retry-After"));
        assertNull(chain.getRequest(), "request must not reach the controller");
    }

    @Test
    void rateLimit_failsOpenWhenRedisIsDown() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForValue()).thenThrow(new RuntimeException("redis down"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new AuthRateLimitFilter(redis, 5).doFilter(new MockHttpServletRequest("POST", "/api/auth/login"), response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    void rateLimit_ignoresNonAuthPaths() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);

        new AuthRateLimitFilter(redis, 5).doFilter(new MockHttpServletRequest("GET", "/api/patients"),
                new MockHttpServletResponse(), new MockFilterChain());

        verifyNoInteractions(redis);
    }
}
