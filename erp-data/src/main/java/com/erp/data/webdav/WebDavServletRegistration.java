package com.erp.data.webdav;

import jakarta.servlet.ServletRegistration;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;

/**
 * 把 {@link WebDavController}（原生 HttpServlet）注册到 /webdav/*。
 *
 * <p>原生 Servlet 不经过 Spring 的 DispatcherServlet，因此能接收 PROPFIND/MKCOL/LOCK
 * 等非标准 HTTP 方法。仍受 Spring Security FilterChain 保护（filter 在 servlet 之前执行）。
 *
 * <p>注意：路径模式用 /webdav/*（单层通配，Servlet 规范），匹配 /webdav 与 /webdav/任意单段。
 * WebDAV 多层路径（/webdav/部门/文件夹/文件）需用 /* 的「路径前缀」语义——Servlet 规范的
 * /webdav/* 仅匹配单段。故同时注册 /webdav/** 风格不可行（Servlet 不支持 **）。
 * 解决：用 {@link jakarta.servlet.annotation.WebServlet} 不便，改为注册路径前缀
 * /webdav/* 并在 controller 内自行解析 getRequestURI() 的完整路径（已如此实现）。
 * 为覆盖多层，注册一个 pathPrefix=/webdav/ 的 Servlet，URL pattern 用 /webdav/* 会被
 * 容器截断为单段——因此改用 {@code addUrlMappings("/webdav/*")} 配合 controller 内
 * 按 {@code request.getRequestURI()} 全路径解析即可（容器仍把 /webdav/a/b/c 路由到本
 * Servlet，因为 /webdav/* 是前缀匹配，.getRequestURI() 返回完整原始路径）。
 */
@Configuration
public class WebDavServletRegistration implements ServletContextInitializer {

    private final WebDavController controller;

    public WebDavServletRegistration(WebDavController controller) {
        this.controller = controller;
    }

    @Override
    public void onStartup(jakarta.servlet.ServletContext servletContext) {
        ServletRegistration.Dynamic reg = servletContext.addServlet("webdavServlet", controller);
        reg.addMapping("/webdav/*");
        reg.setLoadOnStartup(1);
    }
}
