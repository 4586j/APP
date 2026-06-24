
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
class AuthControllerTest {
    @Autowired MockMvc m;
    String login() throws Exception {
        var r = m.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"admin123\"}")).andExpect(status().isOk()).andReturn();
        return com.jayway.jsonpath.JsonPath.read(r.getResponse().getContentAsString(), "$.data.accessToken");
    }
    @Test void login_admin_returnsTokens() throws Exception {
        m.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"admin123\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.accessToken").exists());
    }
    @Test void login_wrongPassword_returnsBusinessError() throws Exception {
        m.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"wrong\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(401));
    }
    @Test void me_withValidToken_returnsUserInfo() throws Exception {
        m.perform(get("/api/v1/auth/me").header("Authorization", "Bearer "+login())).andExpect(status().isOk()).andExpect(jsonPath("$.data.username").value("admin"));
    }
    @Test void logout_withValidToken_returnsOk() throws Exception {
        var r = m.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"admin123\"}")).andReturn();
        String t = com.jayway.jsonpath.JsonPath.read(r.getResponse().getContentAsString(), "$.data.accessToken");
        String rt = com.jayway.jsonpath.JsonPath.read(r.getResponse().getContentAsString(), "$.data.refreshToken");
        m.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer "+t).contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\""+rt+"\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }
    @Test void me_withoutToken_returns401() throws Exception {
        m.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }
}
