package com.erp.security.filter;

import com.erp.security.token.JwtTokenProvider;
import com.erp.security.token.TokenBlacklist;
import com.erp.security.token.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 鉴权过滤器（B1.4 Phase 1）。
 *
 * <p>从请求头 {@code Authorization: Bearer <token>} 取 access token：
 * <ul>
 *   <li>解析失败 / 过期 / 类型不对 → 放行，由后续 401 处理</li>
 *   <li>命中 Redis 黑名单 → 放行（principal 不设置 = 视为匿名）</li>
 *   <li>通过 → 将 username + 角色 authorities 写入 SecurityContext</li>
 * </ul>
 *
 * <p>本过滤器自身不返回 401；让 PermitAll 白名单与 Spring Security 默认入口去裁决。
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklist tokenBlacklist;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, TokenBlacklist tokenBlacklist) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            try {
                Claims claims = jwtTokenProvider.parseToken(token);
                String typeStr = claims.get(JwtTokenProvider.CLAIM_TYPE, String.class);
                if (typeStr == null || TokenType.valueOf(typeStr) != TokenType.ACCESS) {
                    log.debug("非 access token，忽略");
                } else if (tokenBlacklist.isRevoked(claims.getId())) {
                    log.debug("token 已被拉黑 jti={}", claims.getId());
                } else {
                    authenticate(claims);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("JWT 校验失败: {}", ex.getMessage());
            }
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length()).trim();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void authenticate(Claims claims) {
        String username = claims.get(JwtTokenProvider.CLAIM_USERNAME, String.class);
        List<String> roles = (List<String>) claims.getOrDefault(JwtTokenProvider.CLAIM_ROLES, List.of());
        List<String> permissions = (List<String>) claims.getOrDefault(JwtTokenProvider.CLAIM_PERMISSIONS, List.of());
        List<SimpleGrantedAuthority> authorities = java.util.stream.Stream.concat(roles.stream(), permissions.stream())
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        auth.setDetails(claims);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
