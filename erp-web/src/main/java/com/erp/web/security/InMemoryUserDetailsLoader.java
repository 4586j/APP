package com.erp.web.security;

import com.erp.security.user.LoginUser;
import com.erp.security.user.UserDetailsLoader;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component

@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "erp.user.persistence", havingValue = "memory")
public class InMemoryUserDetailsLoader implements UserDetailsLoader {
    private final PasswordEncoder passwordEncoder;
    private final Map<String, LoginUser> users = new HashMap<>();
    public InMemoryUserDetailsLoader(PasswordEncoder passwordEncoder) { this.passwordEncoder = passwordEncoder; }
    @PostConstruct
    void seed() {
        users.put("admin", LoginUser.builder().id(1L).username("admin").encryptedPassword(passwordEncoder.encode("admin123")).realName("系统管理员").department("MANAGEMENT").departmentName("管理部").roles(List.of("ROLE_ADMIN")).permissions(List.of("dashboard:view","order:view","order:create","order:update","order:delete","customer:view","customer:create","user:view","user:create","user:update")).build());
        users.put("sales01", LoginUser.builder().id(2L).username("sales01").encryptedPassword(passwordEncoder.encode("sales123")).realName("销售员张三").department("SALES").departmentName("销售部").roles(List.of("ROLE_SALES")).permissions(List.of("dashboard:view","order:view","order:create","customer:view")).build());
        log.info("InMemoryUserDetailsLoader 已装载 {} 个开发账号: {}", users.size(), users.keySet());
    }
    @Override
    public LoginUser loadByUsername(String username) throws UsernameNotFoundException { LoginUser u = users.get(username); if (u == null) throw new UsernameNotFoundException("not found: " + username); return u; }
    @Override
    public void updatePassword(String username, String encryptedPassword) { LoginUser u = users.get(username); if (u == null) throw new UsernameNotFoundException("not found: " + username); u.setEncryptedPassword(encryptedPassword); }
}
