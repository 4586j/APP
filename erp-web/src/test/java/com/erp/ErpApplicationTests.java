package com.erp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * B1.2 阶段：使用真实 DataSource + Flyway 自动执行迁移。
 * 测试库连接信息由 application.yaml dev profile 提供。
 *
 * <p>Redis 用 MockBean 避免依赖真实 Redis（CI 友好）。
 */
@SpringBootTest
@ActiveProfiles("dev")
class ErpApplicationTests {

    @MockBean
    @SuppressWarnings("unused")
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {
    }
}
