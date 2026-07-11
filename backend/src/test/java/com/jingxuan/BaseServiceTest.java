package com.jingxuan;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Service 层单元测试基类 — Mock 外部依赖（Mapper、Redis 等）
 * 如需加载 Spring 上下文，改用 @SpringBootTest
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class BaseServiceTest {
}
