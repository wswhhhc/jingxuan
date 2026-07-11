package com.jingxuan.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Jackson 配置 — 日期格式、BigInt 转字符串
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern(DATE_TIME_PATTERN),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // LocalDateTime 序列化/反序列化
            builder.serializerByType(LocalDateTime.class,
                    new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
            builder.deserializerByType(LocalDateTime.class,
                    new StdDeserializer<>(LocalDateTime.class) {
                        @Override
                        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt)
                                throws IOException {
                            String text = p.getValueAsString();
                            if (text == null || text.isBlank()) {
                                return null;
                            }
                            for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
                                try {
                                    return LocalDateTime.parse(text, formatter);
                                } catch (DateTimeParseException ignored) {
                                    // Try the next supported format.
                                }
                            }
                            throw ctxt.weirdStringException(text, LocalDateTime.class,
                                    "不支持的时间格式，期望 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-ddTHH:mm:ss");
                        }
                    });

            // Long 转字符串（避免前端精度丢失）
            builder.serializerByType(Long.class, ToStringSerializer.instance);

            // 关闭日期序列化为时间戳
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }
}
