package com.jingxuan.security;

import com.jingxuan.identityaccess.api.RateLimitService;
import com.jingxuan.identityaccess.api.RateLimitUnavailableException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicRateLimitFilterTest {

    private final RateLimitService rateLimitService = mock(RateLimitService.class);
    private final PublicRateLimitFilter filter = new PublicRateLimitFilter(
            new ObjectMapper(), rateLimitService, new TrustedProxyClientIpResolver("127.0.0.1/32,::1/128"));

    @Test
    void trustedPrivateProxyUsesItsSingleRealIpHeader() throws Exception {
        PublicRateLimitFilter dockerFilter = new PublicRateLimitFilter(
                new ObjectMapper(), rateLimitService,
                new TrustedProxyClientIpResolver("172.16.0.0/12"));
        MockHttpServletRequest request = publicRequest("172.18.0.2");
        request.addHeader("X-Real-IP", "203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.99");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(rateLimitService.consume(
                "public-api", "ip:203.0.113.10", 20, Duration.ofSeconds(1)))
                .thenReturn(new RateLimitService.Decision(true, 20, 1));

        dockerFilter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void publicDirectClientCannotSpoofForwardedHeaders() throws Exception {
        MockHttpServletRequest request = publicRequest("203.0.113.10");
        request.addHeader("X-Real-IP", "198.51.100.99");
        request.addHeader("X-Forwarded-For", "198.51.100.99");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(rateLimitService.consume(
                "public-api", "ip:203.0.113.10", 20, Duration.ofSeconds(1)))
                .thenReturn(new RateLimitService.Decision(true, 1, 1));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectedRequestReturnsReal429AndRetryAfter() throws Exception {
        MockHttpServletRequest request = publicRequest("203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(rateLimitService.consume(
                "public-api", "ip:203.0.113.10", 20, Duration.ofSeconds(1)))
                .thenReturn(new RateLimitService.Decision(false, 20, 3));

        filter.doFilter(request, response, chain);

        assertEquals(429, response.getStatus());
        assertEquals("3", response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString().contains("访问过于频繁"));
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void unavailableRateLimitStoreReturns503InsteadOf429() throws Exception {
        MockHttpServletRequest request = publicRequest("203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(rateLimitService.consume(
                "public-api", "ip:203.0.113.10", 20, Duration.ofSeconds(1)))
                .thenThrow(new RateLimitUnavailableException());

        filter.doFilter(request, response, chain);

        assertEquals(503, response.getStatus());
        assertEquals("1", response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString().contains("限流服务暂时不可用"));
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void dynamicAndV1PublicRoutesShareTheSameIpBucket() throws Exception {
        MockHttpServletRequest dynamic = new MockHttpServletRequest("GET", "/public/works/9007199254740993");
        dynamic.setRemoteAddr("203.0.113.10");
        MockHttpServletRequest showcase = new MockHttpServletRequest("GET", "/api/v1/showcase/works/42");
        showcase.setRemoteAddr("203.0.113.10");
        FilterChain chain = mock(FilterChain.class);
        when(rateLimitService.consume("public-api", "ip:203.0.113.10", 20, Duration.ofSeconds(1)))
                .thenReturn(new RateLimitService.Decision(true, 1, 1));

        filter.doFilter(dynamic, new MockHttpServletResponse(), chain);
        filter.doFilter(showcase, new MockHttpServletResponse(), chain);

        verify(rateLimitService, org.mockito.Mockito.times(2))
                .consume("public-api", "ip:203.0.113.10", 20, Duration.ofSeconds(1));
    }

    private static MockHttpServletRequest publicRequest(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/public/works");
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
