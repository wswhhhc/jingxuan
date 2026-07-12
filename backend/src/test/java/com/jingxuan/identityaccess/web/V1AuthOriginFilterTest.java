package com.jingxuan.identityaccess.web;

import com.jingxuan.api.RequestIdFilter;
import com.jingxuan.security.RestAccessDeniedHandler;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class V1AuthOriginFilterTest {

    private static final String SAME_ORIGIN = "http://localhost:5173";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RequestIdFilter requestIdFilter = new RequestIdFilter();
    private final V1AuthOriginFilter originFilter =
            new V1AuthOriginFilter(new RestAccessDeniedHandler(objectMapper));

    @ParameterizedTest(name = "{0} rejects {2} Origin")
    @MethodSource("rejectedOriginRequests")
    void rejectsUnsafeOriginsBeforeReachingBusinessChain(String path, String origin, String label)
            throws Exception {
        MockHttpServletRequest request = authRequest("POST", path);
        if (origin != null) {
            request.addHeader(HttpHeaders.ORIGIN, origin);
        }
        request.addHeader(RequestIdFilter.HEADER, "origin-" + label);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger businessCalls = new AtomicInteger();

        invokeFilters(request, response, (ignoredRequest, ignoredResponse) -> businessCalls.incrementAndGet());

        assertEquals(0, businessCalls.get());
        assertForbiddenProblem(response, "origin-" + label);
        assertNull(response.getHeader(HttpHeaders.SET_COOKIE));
    }

    @ParameterizedTest(name = "same-origin POST reaches {0}")
    @MethodSource("protectedPaths")
    void allowsLegalSameOriginToReachBusinessChain(String path) throws Exception {
        MockHttpServletRequest request = authRequest("POST", path);
        request.addHeader(HttpHeaders.ORIGIN, SAME_ORIGIN);
        request.addHeader(RequestIdFilter.HEADER, "origin-allowed");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger businessCalls = new AtomicInteger();

        invokeFilters(request, response, (ignoredRequest, ignoredResponse) -> businessCalls.incrementAndGet());

        assertEquals(1, businessCalls.get());
        assertEquals(200, response.getStatus());
        assertEquals("origin-allowed", response.getHeader(RequestIdFilter.HEADER));
        assertTrue(response.getContentAsString().isEmpty());
    }

    @Test
    void normalizesDefaultOriginPorts() throws Exception {
        MockHttpServletRequest request = authRequest("POST", "/api/v1/auth/login");
        request.setServerPort(80);
        request.addHeader(HttpHeaders.ORIGIN, "http://localhost");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger businessCalls = new AtomicInteger();

        invokeFilters(request, response, (ignoredRequest, ignoredResponse) -> businessCalls.incrementAndGet());

        assertEquals(1, businessCalls.get());
        assertEquals(200, response.getStatus());
    }

    @ParameterizedTest(name = "rejects malformed Origin {1}")
    @MethodSource("malformedOrigins")
    void rejectsMalformedOrAmbiguousOrigins(String origin, String label) throws Exception {
        MockHttpServletRequest request = authRequest("POST", "/api/v1/auth/login");
        request.addHeader(HttpHeaders.ORIGIN, origin);
        request.addHeader(RequestIdFilter.HEADER, "origin-malformed-" + label);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger businessCalls = new AtomicInteger();

        invokeFilters(request, response, (ignoredRequest, ignoredResponse) -> businessCalls.incrementAndGet());

        assertEquals(0, businessCalls.get());
        assertForbiddenProblem(response, "origin-malformed-" + label);
    }

    @Test
    void rejectsRepeatedOriginHeaders() throws Exception {
        MockHttpServletRequest request = authRequest("POST", "/api/v1/auth/login");
        request.addHeader(HttpHeaders.ORIGIN, SAME_ORIGIN);
        request.addHeader(HttpHeaders.ORIGIN, "https://evil.example");
        request.addHeader(RequestIdFilter.HEADER, "origin-repeated");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger businessCalls = new AtomicInteger();

        invokeFilters(request, response, (ignoredRequest, ignoredResponse) -> businessCalls.incrementAndGet());

        assertEquals(0, businessCalls.get());
        assertForbiddenProblem(response, "origin-repeated");
    }

    @Test
    void forwardedHeadersCannotTurnExternalOriginIntoSameOrigin() throws Exception {
        MockHttpServletRequest request = authRequest("POST", "/api/v1/auth/login");
        request.addHeader(HttpHeaders.ORIGIN, "https://evil.example");
        request.addHeader("Forwarded", "proto=https;host=evil.example");
        request.addHeader("X-Forwarded-Proto", "https");
        request.addHeader("X-Forwarded-Host", "evil.example");
        request.addHeader(RequestIdFilter.HEADER, "origin-forwarded-spoof");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger businessCalls = new AtomicInteger();

        invokeFilters(request, response, (ignoredRequest, ignoredResponse) -> businessCalls.incrementAndGet());

        assertEquals(0, businessCalls.get());
        assertForbiddenProblem(response, "origin-forwarded-spoof");
    }

    @ParameterizedTest(name = "{0} {1} stays outside the auth Origin boundary")
    @MethodSource("unprotectedRequests")
    void onlyFiltersTheThreeAuthenticationPosts(String method, String path) throws Exception {
        MockHttpServletRequest request = authRequest(method, path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicInteger businessCalls = new AtomicInteger();

        invokeFilters(request, response, (ignoredRequest, ignoredResponse) -> businessCalls.incrementAndGet());

        assertEquals(1, businessCalls.get());
        assertEquals(200, response.getStatus());
        assertFalse(response.containsHeader(HttpHeaders.SET_COOKIE));
    }

    private void invokeFilters(MockHttpServletRequest request, MockHttpServletResponse response,
                               FilterChain downstream) throws Exception {
        requestIdFilter.doFilter(request, response,
                (requestWithId, responseWithId) -> originFilter.doFilter(requestWithId, responseWithId, downstream));
    }

    private void assertForbiddenProblem(MockHttpServletResponse response, String requestId) throws Exception {
        assertEquals(403, response.getStatus());
        assertTrue(MediaType.APPLICATION_PROBLEM_JSON.isCompatibleWith(
                MediaType.parseMediaType(response.getContentType())));
        var body = objectMapper.readTree(response.getContentAsString());
        assertEquals(403, body.get("status").asInt());
        assertEquals("FORBIDDEN", body.get("code").asText());
        assertEquals(requestId, body.get("requestId").asText());
        assertEquals(requestId, response.getHeader(RequestIdFilter.HEADER));
    }

    private static MockHttpServletRequest authRequest(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(5173);
        return request;
    }

    private static Stream<Arguments> rejectedOriginRequests() {
        return protectedPathValues().flatMap(path -> Stream.of(
                Arguments.of(path, null, "missing"),
                Arguments.of(path, "", "blank"),
                Arguments.of(path, "null", "null"),
                Arguments.of(path, "https://evil.example", "external")
        ));
    }

    private static Stream<Arguments> protectedPaths() {
        return protectedPathValues().map(Arguments::of);
    }

    private static Stream<String> protectedPathValues() {
        return Stream.of(
                "/api/v1/auth/login",
                "/api/v1/auth/refresh",
                "/api/v1/auth/logout",
                "/api/v1/auth/login;foo=bar",
                "/api/v1/auth/refresh;foo=bar",
                "/api/v1/auth/logout;foo=bar");
    }

    private static Stream<Arguments> malformedOrigins() {
        return Stream.of(
                Arguments.of("https://localhost:5173", "wrong-scheme"),
                Arguments.of("http://localhost:5174", "wrong-port"),
                Arguments.of("http://user@localhost:5173", "userinfo"),
                Arguments.of("http://localhost:5173/path", "path"),
                Arguments.of("http://localhost:5173?query=1", "query"),
                Arguments.of("http://localhost:5173#fragment", "fragment"),
                Arguments.of("http://localhost:5173, https://evil.example", "comma-list")
        );
    }

    private static Stream<Arguments> unprotectedRequests() {
        return Stream.of(
                Arguments.of("GET", "/api/v1/auth/login"),
                Arguments.of("OPTIONS", "/api/v1/auth/login"),
                Arguments.of("POST", "/api/v1/auth/login/extra"),
                Arguments.of("POST", "/auth/login")
        );
    }
}
