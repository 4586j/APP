package com.erp.data.webdav;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.StrictHttpFirewall;

import java.util.List;

/**
 * WebDAV 专用安全链：/webdav/** 走 Basic Auth（由 {@link WebDavAuthFilter} 负责），
 * 独立于主 JWT 链。
 *
 * <p>用独立 {@code SecurityFilterChain}（{@code securityMatcher("/webdav/**")}）接管该路径，
 * 主 {@code SecurityConfig} 的 {@code anyRequest().authenticated()} 不再匹配 webdav，
 * 故无需把 /webdav/** 加入 PUBLIC_PATHS。认证由 filter 内部 401 处理。
 */
@Configuration
public class WebDavSecurityConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain webdavFilterChain(HttpSecurity http,
                                                 WebDavAuthFilter webDavAuthFilter) throws Exception {
        http
                .securityMatcher(request -> {
                    String uri = request.getRequestURI();
                    String contextPath = request.getContextPath();
                    String path = contextPath == null || contextPath.isEmpty()
                            ? uri
                            : uri.substring(contextPath.length());
                    return path.equals("/webdav") || path.startsWith("/webdav/");
                })
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .addFilterBefore(webDavAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public FilterRegistrationBean<WebDavAuthFilter> disableWebDavAuthFilterServletRegistration(
            WebDavAuthFilter webDavAuthFilter) {
        FilterRegistrationBean<WebDavAuthFilter> reg = new FilterRegistrationBean<>(webDavAuthFilter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public StrictHttpFirewall webDavHttpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowedHttpMethods(List.of(
                "GET", "HEAD", "POST", "PUT", "DELETE", "PATCH", "OPTIONS",
                "PROPFIND", "MKCOL", "MOVE", "LOCK", "UNLOCK"));
        return firewall;
    }

    @Bean
    public WebSecurityCustomizer webDavWebSecurityCustomizer(StrictHttpFirewall webDavHttpFirewall) {
        return web -> web.httpFirewall(webDavHttpFirewall);
    }
}
