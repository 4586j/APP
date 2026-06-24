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
class CaptchaControllerTest {
    @Autowired MockMvc m;
    @Test void getCaptcha_returnsImage() throws Exception {
        m.perform(get("/api/v1/auth/captcha")).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.data.imageBase64").exists());
    }
    @Test void getCaptcha_uuidIsNotEmpty() throws Exception {
        m.perform(get("/api/v1/auth/captcha")).andExpect(status().isOk()).andExpect(jsonPath("$.data.uuid").isNotEmpty());
    }
}