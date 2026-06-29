package com.erp.data.webdav;

import com.erp.security.user.LoginUser;
import com.erp.security.user.UserDetailsLoader;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Basic Auth → SecurityContext（WebDAV 专用）。 */
@Slf4j
@Component
public class WebDavAuthFilter extends OncePerRequestFilter {

    private final UserDetailsLoader userDetailsLoader;
    private final PasswordEncoder passwordEncoder;

    public WebDavAuthFilter(UserDetailsLoader userDetailsLoader, PasswordEncoder passwordEncoder) {
        this.userDetailsLoader = userDetailsLoader;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        log.warn("[WebDAV-FILTER] {} {} (enter filter)", request.getMethod(), request.getRequestURI());
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Basic ")) {
            if (tryAuthenticate(header.substring(6).trim())) {
                chain.doFilter(request, response);
                return;
            }
            // 凭证错误 → 401
            challenge(response);
            return;
        }
        // 无 Basic 头 → 401（WebDAV 强制要求认证）
        challenge(response);
    }

    private boolean tryAuthenticate(String b64) {
        try {
            String decoded = new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            if (colon <= 0) return false;
            String username = decoded.substring(0, colon);
            String password = decoded.substring(colon + 1);
            LoginUser user = userDetailsLoader.loadByUsername(username);
            if (user == null) return false;
            if (!passwordEncoder.matches(password, user.getEncryptedPassword())) return false;
            List<SimpleGrantedAuthority> authorities = Stream.concat(
                            user.getRoles() == null ? Stream.<String>empty() : user.getRoles().stream(),
                            user.getPermissions() == null ? Stream.<String>empty() : user.getPermissions().stream())
                    .distinct().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            return true;
        } catch (UsernameNotFoundException ex) {
            return false;
        } catch (IllegalArgumentException ex) {
            return false; // Base64 解码失败
        }
    }

    private void challenge(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Basic realm=\"ERP WebDAV\"");
        response.setContentType("text/plain; charset=utf-8");
        response.getWriter().write("401 Unauthorized");
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/webdav");
    }
}
