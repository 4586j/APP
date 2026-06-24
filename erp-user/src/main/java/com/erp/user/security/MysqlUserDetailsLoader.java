package com.erp.user.security;
import com.erp.security.user.LoginUser;
import com.erp.security.user.UserDetailsLoader;
import com.erp.user.entity.SysDepartment;
import com.erp.user.entity.SysUser;
import com.erp.user.mapper.SysDepartmentMapper;
import com.erp.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component("mysqlUserDetailsLoader")
@Primary
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "erp.user.persistence", havingValue = "mysql", matchIfMissing = true)
public class MysqlUserDetailsLoader implements UserDetailsLoader {
    private final UserService userService;
    private final SysDepartmentMapper deptMapper;
    public MysqlUserDetailsLoader(UserService userService, SysDepartmentMapper deptMapper) {
        this.userService = userService; this.deptMapper = deptMapper;
        log.info("MysqlUserDetailsLoader instantiated");
    }
    @Override public LoginUser loadByUsername(String username) throws UsernameNotFoundException {
        SysUser u = userService.loadByUsername(username);
        if (u == null) throw new UsernameNotFoundException("user not found: " + username);
        if (u.getStatus() != null && u.getStatus() == 0) throw new UsernameNotFoundException("disabled: " + username);
        if (u.getLockedUntil() != null && u.getLockedUntil().isAfter(LocalDateTime.now()))
            throw new UsernameNotFoundException("locked: " + username);
        String deptCode = null, deptName = null;
        SysDepartment dept = deptMapper.selectById(u.getDepartmentId());
        if (dept != null) { deptCode = dept.getCode(); deptName = dept.getName(); }
        List<String> roles = userService.getRolesByUserId(u.getId());
        List<String> permissions = userService.getPermissionsByUserId(u.getId());
        log.info("loaded user {}: roles={}, perms={}", username, roles.size(), permissions.size());
        return new LoginUser(u.getId(), u.getUsername(), u.getPasswordHash(),
            u.getRealName(), deptCode, deptName, roles, permissions);
    }
    @Override public void updatePassword(String username, String encryptedPassword) {
        userService.updatePassword(username, encryptedPassword);
    }
    @Override public void onLoginSuccess(String username, String ip) {
        userService.recordLoginSuccess(username, ip);
    }
    @Override public int onLoginFailure(String username) {
        return userService.recordLoginFailure(username);
    }
}
