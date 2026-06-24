package com.erp.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Captcha 接口端到端测试（B1.4 Phase 2）：
 * <ul>
 *   <li>GET /api/v1/auth/captcha 不需要认证</li>
 *   <li>返回 uuid + data URL PNG</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@ActiveProfiles("dev")
class CaptchaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private final Map<String, String> redisFake = new HashMap<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisFake.clear();
        ValueOperations<String, String> valueOps = org.mockito.Mockito.mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        doAnswer(inv -> {
            redisFake.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(valueOps).set(any(String.class), any(String.class), anyLong(), any(TimeUnit.class));
        when(valueOps.get(any(String.class)))
                .thenAnswer(inv -> redisFake.get((String) inv.getArgument(0)));
        when(stringRedisTemplate.delete(any(String.class)))
                .thenAnswer(inv -> redisFake.remove((String) inv.getArgument(0)) != null);
    }

    @Test
    void captcha_publicEndpoint_returnsUuidAndImage() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.uuid").isNotEmpty())
                .andExpect(jsonPath("$.data.imageBase64").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String uuid = body.path("data").path("uuid").asText();
        String img = body.path("data").path("imageBase64").asText();
        assertNotNull(uuid);
        assertTrue(img.startsWith("data:image/png;base64,"), "imageBase64 必须带 data URL 前缀");
        // 答案已写入 Redis
        assertTrue(redisFake.containsKey("auth:captcha:" + uuid));
    }

    @Test
    void captcha_twoCallsReturnDifferentUuid() throws Exception {
        MvcResult r1 = mockMvc.perform(get("/api/v1/auth/captcha")).andReturn();
        MvcResult r2 = mockMvc.perform(get("/api/v1/auth/captcha")).andReturn();
        String u1 = objectMapper.readTree(r1.getResponse().getContentAsString())
                .path("data").path("uuid").asText();
        String u2 = objectMapper.readTree(r2.getResponse().getContentAsString())
                .path("data").path("uuid").asText();
        assertNotNull(u1);
        assertNotNull(u2);
        assertTrue(!u1.equals(u2), "每次 uuid 必须不同");
    }
}
