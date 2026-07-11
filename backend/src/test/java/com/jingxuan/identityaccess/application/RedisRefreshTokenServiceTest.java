package com.jingxuan.identityaccess.application;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisRefreshTokenServiceTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final RedisRefreshTokenService service = new RedisRefreshTokenService(redis);

    @Test
    void rotationConsumesOldTokenBeforeIssuingReplacement() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.getAndDelete(anyString())).thenReturn("7\tstudent\tSTUDENT\ttrue");
        doNothing().when(values).set(anyString(), anyString(), any(Duration.class));

        RefreshTokenService.RotatedRefreshToken rotated = service.rotate("old-token");

        assertEquals(7L, rotated.userId());
        assertEquals("student", rotated.username());
        assertEquals("STUDENT", rotated.role());
        assertEquals(30 * 24 * 60 * 60, rotated.replacement().expiresIn());
        verify(values).getAndDelete(anyString());
        verify(values).set(anyString(), eq("7\tstudent\tSTUDENT\ttrue"), any(Duration.class));
    }
}
