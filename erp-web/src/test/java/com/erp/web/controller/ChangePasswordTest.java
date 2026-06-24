package com.erp.web.controller;

import com.erp.security.dto.ChangePasswordRequest;
import com.erp.security.dto.LoginRequest;
import com.erp.web.security.InMemoryUserDetailsLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 修改密码集成测试（B1.4 Phase 2）。
 *
 * <p>覆盖：成功、旧密错、新旧同、确认不一致、长度不足。
 * Redis 用 Mock 替身（与 AuthControllerTest 同模式）。
 *
 * <p>注意：InMemoryUserDetailsLoader 是单例 Bean，且 Spring 上下文跨测试类缓存复用。
 * 这里通过 {@code @BeforeEach + @AfterEach} 双向硬重置 admin 密码，
 * 避免污染其它测试类（AuthControllerTest）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@ActiveProfiles("dev")
class ChangePasswordTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryUserDetailsLoader loader;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private final Map<String, String> redisFake = new HashMap<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // 硬重置 admin 密码为 admin123（避免上一轮测试残留）
        loader.updatePassword("admin", passwordEncoder.encode("admin123"));

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

    @AfterEach
    void tearDown() {
        // 再次重置，防止下一类测试受影响
        loader.updatePassword("admin", passwordEncoder.encode("admin123"));
    }

    @Test
    void changePassword_success() throws Exception {
        String token = doLogin("admin", "admin123");
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordRequest(
                                "admin123", "newPass1234", "newPass1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 旧密码已失效（统一 401 message：用户名或密码错误）
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        // 新密码可以登录
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "newPass1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void changePassword_oldPasswordWrong_returns400() throws Exception {
        String token = doLogin("admin", "admin123");
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordRequest(
                                "WRONG-old", "newPass1234", "newPass1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("旧密码不正确"));
    }

    @Test
    void changePassword_newSameAsOld_returns400() throws Exception {
        String token = doLogin("admin", "admin123");
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordRequest(
                                "admin123", "admin123", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("新密码不能与旧密码相同"));
    }

    @Test
    void changePassword_confirmMismatch_returns400() throws Exception {
        String token = doLogin("admin", "admin123");
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordRequest(
                                "admin123", "newPass1234", "DIFFERENT1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("两次输入的新密码不一致"));
    }

    @Test
    void changePassword_tooShort_returns400() throws Exception {
        String token = doLogin("admin", "admin123");
        // 长度 7 — 由 @Size 拦截，GlobalExceptionHandler 翻译成 R(400, ...)
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordRequest(
                                "admin123", "short12", "short12"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    private String doLogin(String user, String pwd) throws Exception {
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
        String accessToken = (String) data.get("accessToken");
        assertNotNull(accessToken);
        return accessToken;
    }
}
