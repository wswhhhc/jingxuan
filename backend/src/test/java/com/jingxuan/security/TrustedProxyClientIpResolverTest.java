package com.jingxuan.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrustedProxyClientIpResolverTest {

    private final TrustedProxyClientIpResolver resolver = new TrustedProxyClientIpResolver("127.0.0.1/32,::1/128");

    @Test
    void onlyExplicitlyTrustedProxyCidrsMaySupplyOneLiteralRealIp() {
        MockHttpServletRequest loopback = request("127.0.0.1");
        loopback.addHeader("X-Real-IP", "203.0.113.8");
        MockHttpServletRequest privateDirect = request("172.18.0.2");
        privateDirect.addHeader("X-Real-IP", "2001:db8::8");

        assertEquals("203.0.113.8", resolver.resolve(loopback));
        assertEquals("172.18.0.2", resolver.resolve(privateDirect));
    }

    @Test
    void explicitlyConfiguredDockerProxyCidrMaySupplyRealIp() {
        TrustedProxyClientIpResolver dockerResolver = new TrustedProxyClientIpResolver(
                "127.0.0.1/32,::1/128,172.16.0.0/12");
        MockHttpServletRequest dockerBridge = request("172.18.0.2");
        dockerBridge.addHeader("X-Real-IP", "2001:db8::8");

        assertEquals("2001:db8:0:0:0:0:0:8", dockerResolver.resolve(dockerBridge));
    }

    @Test
    void publicDirectClientCannotOverrideItsSocketAddress() {
        MockHttpServletRequest request = request("203.0.113.10");
        request.addHeader("X-Real-IP", "198.51.100.99");
        request.addHeader("X-Forwarded-For", "198.51.100.99");

        assertEquals("203.0.113.10", resolver.resolve(request));
    }

    @Test
    void trustedProxyRejectsMultipleOrCommaSeparatedRealIpValues() {
        MockHttpServletRequest multiple = request("127.0.0.1");
        multiple.addHeader("X-Real-IP", "203.0.113.10");
        multiple.addHeader("X-Real-IP", "198.51.100.99");
        MockHttpServletRequest chained = request("127.0.0.1");
        chained.addHeader("X-Real-IP", "203.0.113.10, 198.51.100.99");

        assertEquals("127.0.0.1", resolver.resolve(multiple));
        assertEquals("127.0.0.1", resolver.resolve(chained));
    }

    @Test
    void trustedProxyRejectsHostnamesWithoutDnsResolution() {
        MockHttpServletRequest request = request("127.0.0.1");
        request.addHeader("X-Real-IP", "attacker.example");

        assertEquals("127.0.0.1", resolver.resolve(request));
    }

    private static MockHttpServletRequest request(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
