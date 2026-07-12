package com.jingxuan.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SecurityConfig - 密码编码强度")
class SecurityConfigPasswordEncoderTest {

    @Test
    @DisplayName("新密码使用 BCrypt cost 12")
    void shouldUseBcryptCostTwelve() {
        SecurityConfig securityConfig = new SecurityConfig(null, null, null, null);
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        String encoded = encoder.encode("Maple!73Stone");

        assertTrue(encoded.matches("^\\$2[aby]\\$12\\$.*"));
        assertTrue(encoder.matches("Maple!73Stone", encoded));
    }

    @Test
    @DisplayName("cost 12 编码器兼容验证历史 cost 10 哈希")
    void shouldMatchLegacyBcryptCostTenHash() {
        String legacyHash = new BCryptPasswordEncoder(10).encode("Legacy84Password");
        PasswordEncoder encoder = new SecurityConfig(null, null, null, null).passwordEncoder();

        assertTrue(encoder.matches("Legacy84Password", legacyHash));
    }
}
