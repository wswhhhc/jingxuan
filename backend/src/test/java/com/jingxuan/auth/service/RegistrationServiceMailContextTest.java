package com.jingxuan.auth.service;

import com.jingxuan.config.MailConfig;
import com.jingxuan.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RegistrationServiceMailContextTest {

    @Test
    void startsWithoutMailCredentialsAndKeepsRegistrationAvailable() {
        new ApplicationContextRunner()
                .withUserConfiguration(MailConfig.class, RegistrationService.class)
                .withBean(SysUserMapper.class, () -> mock(SysUserMapper.class))
                .withBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class))
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .withPropertyValues("MAIL_USERNAME=", "MAIL_PASSWORD=")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(RegistrationService.class);
                    assertThat(context.getBeanProvider(JavaMailSender.class).getIfAvailable()).isNull();
                });
    }
}
