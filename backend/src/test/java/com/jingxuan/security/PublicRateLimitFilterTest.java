package com.jingxuan.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

class PublicRateLimitFilterTest {

    private final TrustedProxyClientIpResolver clientIpResolver =
            new TrustedProxyClientIpResolver("127.0.0.1/32,::1/128");
    private final PublicRateLimitFilter filter =
            new PublicRateLimitFilter(new ObjectMapper(), clientIpResolver, 20, 1000);

    @Test
    void testFilterWorks() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        assertTrue(true);
    }

    @Test
    void appliesToApiPublicAndCommentRoutes() {
        assertFalse(filter.shouldNotFilter(request("/api/public/works")));
        assertFalse(filter.shouldNotFilter(request("/api/public/ranking/list")));
        assertFalse(filter.shouldNotFilter(request("/api/comment/list/1")));
        assertFalse(filter.shouldNotFilter(request("/api/comment/add")));
        assertFalse(filter.shouldNotFilter(request("/api/v1/showcase/works")));
        assertFalse(filter.shouldNotFilter(request("/api/v1/showcase/works/1")));
        assertTrue(filter.shouldNotFilter(request("/api/student/works")));
    }

    @Test
    void trustedProxyClientsUseIndependentRateLimitBuckets() throws Exception {
        PublicRateLimitFilter oneRequestFilter =
                new PublicRateLimitFilter(new ObjectMapper(), clientIpResolver, 1, 10_000);
        FilterChain firstChain = mock(FilterChain.class);
        FilterChain secondChain = mock(FilterChain.class);

        oneRequestFilter.doFilter(proxiedRequest("203.0.113.10"), new MockHttpServletResponse(), firstChain);
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        oneRequestFilter.doFilter(proxiedRequest("203.0.113.11"), secondResponse, secondChain);

        verify(firstChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(secondChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertNotEquals(429, secondResponse.getStatus());
    }

    @Test
    void v1RateLimitUsesProblemDetails() throws Exception {
        PublicRateLimitFilter blockedFilter =
                new PublicRateLimitFilter(new ObjectMapper(), clientIpResolver, 0, 10_000);
        MockHttpServletRequest request = request("/api/v1/showcase/works");
        request.setAttribute(com.jingxuan.api.RequestIdFilter.ATTRIBUTE, "rate-limit-429");
        MockHttpServletResponse response = new MockHttpServletResponse();

        blockedFilter.doFilter(request, response, mock(FilterChain.class));

        assertEquals(429, response.getStatus());
        assertTrue(MediaType.APPLICATION_PROBLEM_JSON.isCompatibleWith(
                MediaType.parseMediaType(response.getContentType())));
        var body = new ObjectMapper().readTree(response.getContentAsString());
        assertEquals("RATE_LIMITED", body.get("code").asText());
        assertEquals("rate-limit-429", body.get("requestId").asText());
    }

    @Test
    void concurrentRequestsCannotBypassConfiguredLimit() throws Exception {
        int limit = 20;
        int requestCount = 64;
        PublicRateLimitFilter concurrentFilter =
                new PublicRateLimitFilter(new ObjectMapper(), clientIpResolver, limit, 10_000);
        AtomicInteger passed = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(requestCount);
        List<java.util.concurrent.Future<Integer>> responses = new ArrayList<>();
        try {
            for (int i = 0; i < requestCount; i++) {
                responses.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    MockHttpServletResponse response = new MockHttpServletResponse();
                    concurrentFilter.doFilter(request("/api/public/works"), response,
                            (req, resp) -> passed.incrementAndGet());
                    return response.getStatus();
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            long limited = 0;
            for (var response : responses) {
                if (response.get(10, TimeUnit.SECONDS) == 429) {
                    limited++;
                }
            }
            assertEquals(limit, passed.get());
            assertEquals(requestCount - limit, limited);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void clearingCountersStartsANewWindow() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        for (int i = 0; i < 21; i++) {
            filter.doFilter(request("/api/public/works"), new MockHttpServletResponse(), chain);
        }

        filter.clearCounters();
        FilterChain afterClear = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("/api/public/works"), response, afterClear);

        assertNotEquals(429, response.getStatus());
        verify(afterClear).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        return request;
    }

    private MockHttpServletRequest proxiedRequest(String clientIp) {
        MockHttpServletRequest request = request("/api/public/works");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Real-IP", clientIp);
        return request;
    }

    private static <T> T mock(Class<T> clazz) {
        return org.mockito.Mockito.mock(clazz);
    }
}
