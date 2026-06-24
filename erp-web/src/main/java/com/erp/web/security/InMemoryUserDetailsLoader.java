package com.erp.web.security;

import com.erp.security.user.LoginUser;
import com.erp.security.user.UserDetailsLoader;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 开发期内存用户加载器（B1.4 Phase 1）。
 *
 * <p>仅在 dev profile 启用；B1.2 装好 MySQL + erp-user 模块后，由真实实现替换。
 * 内置两个固定账号：
 * <ul>
 *   <li>admin / admin123 — ROLE_ADMIN，全权限</li>
 *   <li>sales01 / sales123 — ROLE_SALES，业务权限</li>
 * </ul>
 */
@Slf4j
@Component
@Profile("dev")
public class InMemoryUserDetailsLoader implements UserDetailsLoader {

    private final PasswordEncoder passwordEncoder;
    private final Map<String, LoginUser> users = new HashMap<>();

    public InMemoryUserDetailsLoader(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    void seed() {
        users.put("admin", LoginUser.builder()
                .id(1L)
                .username("admin")
                .encryptedPassword(passwordEncoder.encode("admin123"))
                .realName("系统管理员")
                .department("MANAGEMENT")
                .departmentName("管理部")
                .roles(List.of("ROLE_ADMIN"))
                .permissions(List.of(
                        "dashboard:view",
                        "order:view", "order:create", "order:update", "order:delete",
                        "customer:view", "customer:create",
                        "user:view", "user:create", "user:update"))
                .build());

        users.put("sales01", LoginUser.builder()
                .id(2L)
                .username("sales01")
                .encryptedPassword(passwordEncoder.encode("sales123"))
                .realName("销售员张三")
                .department("SALES")
                .departmentName("销售部")
                .roles(List.of("ROLE_SALES"))
                .permissions(List.of(
                        "dashboard:view",
                        "order:view", "order:create",
                        "customer:view"))
                .build());

        log.info("InMemoryUserDetailsLoader 已装载 {} 个开发账号: {}",
                users.size(), users.keySet());
    }

    @Override
    public LoginUser loadByUsername(String username) throws UsernameNotFoundException {
        LoginUser u = users.get(username);
        if (u == null) {
            throw new UsernameNotFoundException("user not found: " + username);
        }
        return u;
    }
}
