package com.jingxuan.identityaccess.web;

import com.jingxuan.exception.UnauthorizedException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshCookieManagerTest {

    private static final String COOKIE_NAME = "jingxuan_refresh";

    private final RefreshCookieManager manager = new RefreshCookieManager();

    @Test
    void writesLockedRefreshCookieAttributes() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        manager.write(response, "opaque-refresh-token", 28800);

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.startsWith(COOKIE_NAME + "=opaque-refresh-token;"));
        assertLockedAttributes(setCookie);
        assertTrue(setCookie.contains("Max-Age=28800"));
    }

    @Test
    void clearsCookieWithTheSameLockedAttributes() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        manager.clear(response);

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.startsWith(COOKIE_NAME + "=;"));
        assertLockedAttributes(setCookie);
        assertTrue(setCookie.contains("Max-Age=0"));
    }

    @Test
    void requiresNamedCookieAndIgnoresOtherCookies() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie("other", "unrelated"),
                new Cookie(COOKIE_NAME, "opaque-refresh-token"));

        assertEquals("opaque-refresh-token", manager.require(request));
    }

    @Test
    void rejectsMissingCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThrows(UnauthorizedException.class, () -> manager.require(request));
    }

    @Test
    void optionalReadTreatsMissingCookieAsAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertTrue(manager.find(request).isEmpty());
    }

    @Test
    void rejectsBlankCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(COOKIE_NAME, ""));

        assertThrows(UnauthorizedException.class, () -> manager.require(request));
    }

    @Test
    void rejectsAmbiguousDuplicateRefreshCookies() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie(COOKIE_NAME, "first-token"),
                new Cookie(COOKIE_NAME, "second-token"));

        assertThrows(UnauthorizedException.class, () -> manager.require(request));
        assertTrue(manager.find(request).isEmpty());
    }

    private static void assertLockedAttributes(String setCookie) {
        assertTrue(setCookie.contains("Path=/api/v1/auth"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("SameSite=Strict"));
        assertFalse(setCookie.contains("; Secure"));
        assertFalse(setCookie.contains("Domain="));
    }
}
