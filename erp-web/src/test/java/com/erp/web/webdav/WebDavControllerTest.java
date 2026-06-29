package com.erp.web.webdav;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.erp.data.service.DatFileService;
import com.erp.data.webdav.ResolvedPath;
import com.erp.data.webdav.WebDavPathResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WebDAV 协议动词 → 状态码/XML 集成测试。
 *
 * <p>沿用 erp-web 既有测试的上下文裁剪方式（排除数据源/Flyway/MyBatis，内存用户）。
 * 注意：本环境因 erp-product 模块 mapper 依赖，全上下文 @SpringBootTest 暂无法加载
 * （与 AuthControllerTest 同一预先存在的环境限制）；在具备完整测试上下文的环境中可运行。
 */
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class, MybatisPlusAutoConfiguration.class})
@ActiveProfiles("dev")
@org.springframework.test.context.TestPropertySource(properties = "erp.user.persistence=memory")
@SpringBootTest
class WebDavControllerTest {

    @Autowired MockMvc m;
    @MockBean DatFileService fileService;
    @MockBean WebDavPathResolver resolver;
    @MockBean JdbcTemplate jdbcTemplate;

    private String basic(String user, String pass) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void noAuth_returns401() throws Exception {
        m.perform(propfind("/webdav/")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPassword_returns401() throws Exception {
        m.perform(propfind("/webdav/").header("Authorization", basic("admin", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void options_returnsDavHeader() throws Exception {
        m.perform(request("OPTIONS", "/webdav/").header("Authorization", basic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(header().string("DAV", "1,2"));
    }

    @Test
    void propfind_root_returns207() throws Exception {
        when(resolver.resolve(any(), any())).thenReturn(ResolvedPath.root("/webdav/"));
        when(fileService.listFiles(any(), any())).thenReturn(List.of());
        m.perform(propfind("/webdav/").header("Authorization", basic("admin", "admin123")))
                .andExpect(status().is(207))
                .andExpect(content().contentTypeCompatibleWith("application/xml"));
    }

    @Test
    void propfind_notFound_returns404() throws Exception {
        when(resolver.resolve(any(), any())).thenReturn(ResolvedPath.notFound("/webdav/x/"));
        m.perform(propfind("/webdav/x/").header("Authorization", basic("admin", "admin123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void mkcol_forbidden_returns403() throws Exception {
        when(resolver.resolve(any(), any())).thenReturn(ResolvedPath.root("/webdav/"));
        when(fileService.canCreate(any(), any())).thenReturn(false);
        m.perform(request("MKCOL", "/webdav/销售部/新文件夹").header("Authorization", basic("admin", "admin123")))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder propfind(String url) {
        return request("PROPFIND", url).contentType(MediaType.APPLICATION_XML).content("<x/>");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request(String method, String url) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                org.springframework.http.HttpMethod.valueOf(method), url);
    }
}