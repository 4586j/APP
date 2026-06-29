package com.erp.web.webdav;

import com.erp.data.service.DatFileService;
import com.erp.data.webdav.ResolvedPath;
import com.erp.data.webdav.WebDavController;
import com.erp.data.webdav.WebDavLockStore;
import com.erp.data.webdav.WebDavPathResolver;
import com.erp.data.webdav.WebDavPropFindXmlBuilder;
import com.erp.security.user.LoginUser;
import com.erp.security.user.UserDetailsLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * WebDavController 协议动词分发单测。
 *
 * <p>直接构造 MockHttpServletRequest 调用 {@link WebDavController#handleRequest}，
 * 验证各 WebDAV 动词（含 PROPFIND/MKCOL 等非标准方法）的状态码映射——
 * 这是 {@code @RequestMapping} 架构无法覆盖的（Spring MVC 对非标准方法返回 400）。
 * 鉴权 401 由 WebDavAuthFilter 负责，已在 erp-data 层外，此处聚焦 controller 分发。
 */
class WebDavControllerTest {

    private final WebDavPathResolver resolver = mock(WebDavPathResolver.class);
    private final DatFileService fileService = mock(DatFileService.class);
    private final WebDavPropFindXmlBuilder xmlBuilder = new WebDavPropFindXmlBuilder();
    private final WebDavLockStore lockStore = new WebDavLockStore();
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final UserDetailsLoader userDetailsLoader = mock(UserDetailsLoader.class);

    private final WebDavController controller = new WebDavController(
            resolver, fileService, xmlBuilder, lockStore, jdbcTemplate, userDetailsLoader);

    private LoginUser admin() {
        return LoginUser.builder().id(1L).username("admin").departmentId(1L)
                .roles(List.of("ROLE_ADMIN")).build();
    }

    private void loginAs(LoginUser user) {
        when(userDetailsLoader.loadByUsername("admin")).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletResponse handle(String method, String uri) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod(method);
        req.setRequestURI(uri);
        if ("PROPFIND".equals(method)) {
            req.setContentType("application/xml");
            req.setContent("<x/>".getBytes());
        }
        MockHttpServletResponse resp = new MockHttpServletResponse();
        controller.service(req, resp);
        return resp;
    }

    @Test
    void options_returns200AndDavHeader() throws Exception {
        loginAs(admin());
        MockHttpServletResponse resp = handle("OPTIONS", "/webdav/");
        assertEquals(200, resp.getStatus());
        assertEquals("1,2", resp.getHeader("DAV"));
    }

    @Test
    void propfind_root_returns207() throws Exception {
        loginAs(admin());
        when(resolver.resolve(any(), any())).thenReturn(ResolvedPath.root("/webdav/"));
        when(fileService.listFiles(any(), any())).thenReturn(List.of());
        when(jdbcTemplate.queryForList(any(String.class))).thenReturn(List.of());
        MockHttpServletResponse resp = handle("PROPFIND", "/webdav/");
        assertEquals(207, resp.getStatus());
        assertTrue(resp.getContentType().startsWith("application/xml"));
    }

    @Test
    void propfind_notFound_returns404() throws Exception {
        loginAs(admin());
        when(resolver.resolve(any(), any())).thenReturn(ResolvedPath.notFound("/webdav/x/"));
        MockHttpServletResponse resp = handle("PROPFIND", "/webdav/x/");
        assertEquals(404, resp.getStatus());
    }

    @Test
    void mkcol_forbidden_returns403() throws Exception {
        loginAs(admin());
        when(resolver.resolve(any(), any())).thenReturn(ResolvedPath.root("/webdav/"));
        when(fileService.canCreate(any(), any())).thenReturn(false);
        MockHttpServletResponse resp = handle("MKCOL", "/webdav/销售部/新文件夹");
        assertEquals(403, resp.getStatus());
    }

    @Test
    void unknownMethod_returns405() throws Exception {
        loginAs(admin());
        MockHttpServletResponse resp = handle("TRACE", "/webdav/");
        assertEquals(405, resp.getStatus());
    }
}