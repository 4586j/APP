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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class,FlywayAutoConfiguration.class,HibernateJpaAutoConfiguration.class,MybatisPlusAutoConfiguration.class})
@ActiveProfiles("dev")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
class ChangePasswordTest {
    @Autowired MockMvc m;
    String login() throws Exception {
        var r = m.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"admin123\"}")).andExpect(status().isOk()).andReturn();
        return com.jayway.jsonpath.JsonPath.read(r.getResponse().getContentAsString(), "$.data.accessToken");
    }
    @Test void changePassword_success() throws Exception {
        m.perform(post("/api/v1/auth/change-password").header("Authorization", "Bearer "+login()).contentType(MediaType.APPLICATION_JSON).content("{\"oldPassword\":\"admin123\",\"newPassword\":\"newPass456\",\"confirmPassword\":\"newPass456\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }
    @Test void changePassword_wrongOldPassword_fails() throws Exception {
        m.perform(post("/api/v1/auth/change-password").header("Authorization", "Bearer "+login()).contentType(MediaType.APPLICATION_JSON).content("{\"oldPassword\":\"wrongOld\",\"newPassword\":\"newPass456\",\"confirmPassword\":\"newPass456\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }
    @Test void changePassword_notLoggedIn_returnsUnauthorized() throws Exception {
        m.perform(post("/api/v1/auth/change-password").contentType(MediaType.APPLICATION_JSON).content("{\"oldPassword\":\"admin123\",\"newPassword\":\"newPass456\",\"confirmPassword\":\"newPass456\"}")).andExpect(status().isUnauthorized());
    }
    @Test void changePassword_mismatchedConfirmPassword_fails() throws Exception {
        m.perform(post("/api/v1/auth/change-password").header("Authorization", "Bearer "+login()).contentType(MediaType.APPLICATION_JSON).content("{\"oldPassword\":\"admin123\",\"newPassword\":\"newPass456\",\"confirmPassword\":\"different\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }
}