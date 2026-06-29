package com.erp.data.webdav;

import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
    @Order(SecurityProperties.DEFAULT_FILTER_ORDER - 1)
    public SecurityFilterChain webdavFilterChain(HttpSecurity http,
                                                 WebDavAuthFilter webDavAuthFilter) throws Exception {
        http
                .securityMatcher("/webdav/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .addFilterBefore(webDavAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
