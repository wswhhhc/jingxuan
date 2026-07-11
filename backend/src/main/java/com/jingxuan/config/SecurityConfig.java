package com.jingxuan.config;

import com.jingxuan.security.JwtAuthenticationFilter;
import com.jingxuan.security.PublicRateLimitFilter;
import com.jingxuan.security.RestAccessDeniedHandler;
import com.jingxuan.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PublicRateLimitFilter publicRateLimitFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(exception -> exception
                    .authenticationEntryPoint(restAuthenticationEntryPoint)
                    .accessDeniedHandler(restAccessDeniedHandler))
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // Knife4j / Swagger
                    .requestMatchers("/doc.html", "/swagger-ui/**", "/v3/api-docs/**",
                            "/webjars/**", "/favicon.ico").permitAll()
                    // 认证接口（兼容前端 /api 代理）
                    .requestMatchers("/auth/login", "/api/auth/login",
                            "/auth/register", "/api/auth/register",
                            "/auth/send-code", "/api/auth/send-code").permitAll()
                    // 静态资源
                    .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                    // 前台公开展示接口
                    .requestMatchers("/public/**").permitAll()
                    // 公共评论列表 & 发表（游客也可评论）
                    .requestMatchers(HttpMethod.GET, "/comment/list/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/comment/add", "/api/comment/add").permitAll()
                    // 其它接口需认证
                    .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(publicRateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
