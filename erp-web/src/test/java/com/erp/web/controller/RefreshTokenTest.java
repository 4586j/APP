package com.erp.web.controller;

import com.erp.security.dto.LoginRequest;
import com.erp.security.dto.RefreshTokenRequest;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * refresh-token 集成测试（B1.4 Phase 2）。
 *
 * <p>覆盖：refresh 成功（拿到新 access + 新 refresh）、旧 refresh 拉黑后失效、
 * access token 不能当 refresh 用。
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@ActiveProfiles("dev")
class RefreshTokenTest {

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
        when(stringRedisTemplate.hasKey(any(String.class)))
                .thenAnswer(inv -> redisFake.containsKey((String) inv.getArgument(0)));
        when(stringRedisTemplate.delete(any(String.class)))
                .thenAnswer(inv -> redisFake.remove((String) inv.getArgument(0)) != null);
    }

    @Test
    void refresh_success_returnsNewAccessAndRefresh() throws Exception {
        Map<String, Object> tokens = doLogin("admin", "admin123");
        String access = (String) tokens.get("accessToken");
        String refresh = (String) tokens.get("refreshToken");

        Map<String, Object> renewed = doRefresh(refresh);
        String newAccess = (String) renewed.get("accessToken");
        String newRefresh = (String) renewed.get("refreshToken");

        assertNotNull(newAccess);
        assertNotNull(newRefresh);
        // jti 必须变（jti 随机 UUID，token 字符串不会重复）
        assertNotEquals(access, newAccess, "新 access 必须 ≠ 旧 access");
        assertNotEquals(refresh, newRefresh, "新 refresh 必须 ≠ 旧 refresh");
    }

    @Test
    void refresh_oldRefreshBlacklistedAfterUse_returns401() throws Exception {
        Map<String, Object> tokens = doLogin("admin", "admin123");
        String refresh = (String) tokens.get("refreshToken");

        // 第一次成功
        doRefresh(refresh);

        // 第二次用同一个旧 refresh — 应失败（已加黑名单）
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refresh))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("refresh token 已被撤销"));
    }

    @Test
    void refresh_withAccessToken_returns401() throws Exception {
        Map<String, Object> tokens = doLogin("admin", "admin123");
        String access = (String) tokens.get("accessToken");

        // 用 access token 当 refresh — 应失败
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(access))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("token 类型不正确，必须使用 refresh token"));
    }

    @Test
    void refresh_viaAuthorizationHeader_alsoWorks() throws Exception {
        Map<String, Object> tokens = doLogin("sales01", "sales123");
        String refresh = (String) tokens.get("refreshToken");

        // body 为空 JSON，从 Header 取（@RequestBody required=false 允许空 body，但 Spring 仍需 content-type 兼容）
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Bearer " + refresh)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.userInfo.username").value("sales01"));
    }

    private Map<String, Object> doLogin(String user, String pwd) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(user, pwd))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(json, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        return data;
    }

    private Map<String, Object> doRefresh(String refresh) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refresh))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(json, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        return data;
    }
}
