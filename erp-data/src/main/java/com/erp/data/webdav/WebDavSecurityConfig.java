package com.erp.data.webdav;

import com.erp.security.filter.JwtAuthenticationFilter;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 注册 WebDavAuthFilter 在 JwtAuthenticationFilter 之前，
 * 使 /webdav/** 走 Basic Auth。
 */
@Configuration
public class WebDavSecurityConfig {

    @Bean
    @Order(SecurityProperties.DEFAULT_FILTER_ORDER - 1)
    public SecurityFilterChain webdavFilterChain(HttpSecurity http,
                                                 JwtAuthenticationFilter jwtFilter,
                                                 WebDavAuthFilter webDavAuthFilter) throws Exception {
        http
                .securityMatcher("/webdav/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .addFilterBefore(webDavAuthFilter, JwtAuthenticationFilter.class);
        return http.build();
    }
}
