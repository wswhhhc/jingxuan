package com.jingxuan.security;

import com.jingxuan.api.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestSecurityErrorHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestAuthenticationEntryPoint authenticationEntryPoint =
            new RestAuthenticationEntryPoint(objectMapper);
    private final RestAccessDeniedHandler accessDeniedHandler =
            new RestAccessDeniedHandler(objectMapper);

    @Test
    void unauthenticatedV1RequestUsesProblemDetails() throws Exception {
        MockHttpServletRequest request = request("/api/v1/me/works", "unit-401");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(request, response,
                new InsufficientAuthenticationException("missing token"));

        assertEquals(401, response.getStatus());
        assertProblemJson(response);
        var body = objectMapper.readTree(response.getContentAsString());
        assertEquals(401, body.get("status").asInt());
        assertEquals("UNAUTHENTICATED", body.get("code").asText());
        assertEquals("unit-401", body.get("requestId").asText());
    }

    @Test
    void forbiddenV1RequestUsesProblemDetails() throws Exception {
        MockHttpServletRequest request = request("/api/v1/tags/1/deletion-impact", "unit-403");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("denied"));

        assertEquals(403, response.getStatus());
        assertProblemJson(response);
        var body = objectMapper.readTree(response.getContentAsString());
        assertEquals(403, body.get("status").asInt());
        assertEquals("FORBIDDEN", body.get("code").asText());
        assertEquals("unit-403", body.get("requestId").asText());
    }

    @Test
    void legacySecurityErrorsKeepResultEnvelope() throws Exception {
        MockHttpServletRequest request = request("/api/student/tasks", "legacy-request");
        MockHttpServletResponse unauthenticated = new MockHttpServletResponse();
        MockHttpServletResponse forbidden = new MockHttpServletResponse();

        authenticationEntryPoint.commence(request, unauthenticated,
                new InsufficientAuthenticationException("missing token"));
        accessDeniedHandler.handle(request, forbidden, new AccessDeniedException("denied"));

        assertJson(unauthenticated);
        assertEquals(401, objectMapper.readTree(unauthenticated.getContentAsString()).get("code").asInt());
        assertJson(forbidden);
        assertEquals(403, objectMapper.readTree(forbidden.getContentAsString()).get("code").asInt());
    }

    private MockHttpServletRequest request(String uri, String requestId) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        request.setAttribute(RequestIdFilter.ATTRIBUTE, requestId);
        return request;
    }

    private void assertProblemJson(MockHttpServletResponse response) {
        assertTrue(MediaType.APPLICATION_PROBLEM_JSON.isCompatibleWith(
                MediaType.parseMediaType(response.getContentType())));
    }

    private void assertJson(MockHttpServletResponse response) {
        assertTrue(MediaType.APPLICATION_JSON.isCompatibleWith(
                MediaType.parseMediaType(response.getContentType())));
    }
}
