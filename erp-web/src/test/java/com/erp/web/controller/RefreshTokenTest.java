package com.erp.web.controller;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class,FlywayAutoConfiguration.class,HibernateJpaAutoConfiguration.class,MybatisPlusAutoConfiguration.class})
@ActiveProfiles("dev")
@SpringBootTest
class RefreshTokenTest {
    @Autowired MockMvc m;
    String login() throws Exception {
        var r = m.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"admin123\"}")).andExpect(status().isOk()).andReturn();
        return com.jayway.jsonpath.JsonPath.read(r.getResponse().getContentAsString(), "$.data.refreshToken");
    }
    @Test void refresh_success_returnsNewAccessAndRefresh() throws Exception {
        m.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\""+login()+"\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.accessToken").exists());
    }
    @Test void refresh_withAccessToken_returns401() throws Exception {
        m.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"bad\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
    }
    @Test void refresh_oldRefreshBlacklistedAfterUse_returns401() throws Exception {
        String rt = login();
        m.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\""+rt+"\"}"));
        m.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\""+rt+"\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
    }
    @Test void refresh_viaAuthorizationHeader_alsoWorks() throws Exception {
        m.perform(post("/api/v1/auth/refresh").header("Authorization", "Bearer "+login()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$.data.accessToken").exists());
    }
}