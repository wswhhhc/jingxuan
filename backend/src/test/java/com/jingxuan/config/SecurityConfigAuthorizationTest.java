package com.jingxuan.config;

import com.jingxuan.identityaccess.api.RateLimitService;
import com.jingxuan.security.CustomUserDetailsService;
import com.jingxuan.security.JwtAuthenticationFilter;
import com.jingxuan.security.JwtTokenProvider;
import com.jingxuan.security.PublicRateLimitFilter;
import com.jingxuan.security.RestAccessDeniedHandler;
import com.jingxuan.security.RestAuthenticationEntryPoint;
import com.jingxuan.security.TokenBlacklistService;
import com.jingxuan.security.TrustedProxyClientIpResolver;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigAuthorizationTest {

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfiguration.class);
        context.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    void postChallengeIsPublicButOtherMethodsRemainAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/auth/challenges"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/auth/challenges"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationAndEmailVerificationArePublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/email-verifications"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/registrations"))
                .andExpect(status().isCreated());
    }

    @Test
    void errorDispatcherIsPublicButDirectErrorRequestRemainsAuthenticated() throws Exception {
        mockMvc.perform(get("/error").with(request -> {
                    request.setDispatcherType(DispatcherType.ERROR);
                    return request;
                }))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/error"))
                .andExpect(status().isUnauthorized());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class TestConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        CustomUserDetailsService customUserDetailsService() {
            return mock(CustomUserDetailsService.class);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(
                CustomUserDetailsService customUserDetailsService) {
            return new JwtAuthenticationFilter(mock(JwtTokenProvider.class),
                    customUserDetailsService, mock(TokenBlacklistService.class), List.of());
        }

        @Bean
        PublicRateLimitFilter publicRateLimitFilter(
                ObjectMapper objectMapper, TrustedProxyClientIpResolver clientIpResolver) {
            return new PublicRateLimitFilter(objectMapper, clientIpResolver, 20, 1000);
        }

        @Bean
        TrustedProxyClientIpResolver trustedProxyClientIpResolver() {
            return new TrustedProxyClientIpResolver("127.0.0.1/32,::1/128");
        }

        @Bean
        RestAuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper objectMapper) {
            return new RestAuthenticationEntryPoint(objectMapper);
        }

        @Bean
        RestAccessDeniedHandler restAccessDeniedHandler(ObjectMapper objectMapper) {
            return new RestAccessDeniedHandler(objectMapper);
        }

        @Bean
        ProbeController probeController() {
            return new ProbeController();
        }
    }

    @RestController
    static class ProbeController {

        @PostMapping("/api/v1/auth/challenges")
        @ResponseStatus(HttpStatus.CREATED)
        void createChallenge() {
        }

        @GetMapping("/api/v1/auth/challenges")
        void getChallenge() {
        }

        @PostMapping("/api/v1/auth/email-verifications")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void createEmailVerification() {
        }

        @PostMapping("/api/v1/auth/registrations")
        @ResponseStatus(HttpStatus.CREATED)
        void createRegistration() {
        }

        @RequestMapping("/error")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void error() {
        }
    }
}

