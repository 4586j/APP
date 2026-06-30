package com.erp.data.webdav;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Handles Windows WebClient's initial root OPTIONS probe before it accesses /webdav.
 */
public class WebDavWindowsOptionsFilter implements Filter {

    private static final String METHODS = "OPTIONS, PROPFIND, GET, PUT, MKCOL, DELETE, MOVE, LOCK, UNLOCK";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        if ("OPTIONS".equals(req.getMethod()) && isRootPath(req) && isWindowsWebDav(req)) {
            resp.setHeader("DAV", "1,2");
            resp.setHeader("Allow", METHODS);
            resp.setHeader("Public", METHODS);
            resp.setHeader("MS-Author-Via", "DAV");
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isRootPath(HttpServletRequest req) {
        String path = req.getRequestURI();
        String contextPath = req.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path.isEmpty() || "/".equals(path);
    }

    private boolean isWindowsWebDav(HttpServletRequest req) {
        String userAgent = req.getHeader("User-Agent");
        return userAgent != null && userAgent.contains("Microsoft-WebDAV-MiniRedir");
    }
}
