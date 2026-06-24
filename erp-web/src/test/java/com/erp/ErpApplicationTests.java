package com.erp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * B0.3 阶段 contextLoads 测试。
 * 排除 DataSource / Flyway / JPA 自动配置（B1.1 配好 MySQL 后改回完整启动）。
 *
 * <p>B1.4 Phase 1：激活 dev profile 让 InMemoryUserDetailsLoader 装配，
 * 同时 Mock StringRedisTemplate 避免依赖真实 Redis。
 */
@SpringBootTest
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@ActiveProfiles("dev")
class ErpApplicationTests {

    @MockBean
    @SuppressWarnings("unused")
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {
    }
}
