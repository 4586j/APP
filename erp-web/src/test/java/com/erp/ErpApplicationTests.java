package com.erp;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
@org.springframework.test.context.TestPropertySource(properties = "erp.user.persistence=memory")
@SpringBootTest @ActiveProfiles("dev")
class ErpApplicationTests {
    @MockBean private StringRedisTemplate stringRedisTemplate;
    @Test void contextLoads() {}
}