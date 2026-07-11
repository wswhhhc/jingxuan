package com.jingxuan.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Jackson 配置 — 先保证 Boot 4 可编译，保留 Long 转字符串与禁用时间戳。
 *
 * <p>阶段 1 仅完成工程骨架迁移；原有 LocalDateTime 的定制格式将在后续契约收敛时
 * 一并迁移到 Jackson 3 的完整模块配置。</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            builder.addModule(module);
        };
    }
}
