package com.erp.security.user;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * 用户信息加载器抽象（B1.4 Phase 1）。
 *
 * <p>erp-security 自身不依赖 MySQL/erp-user。本接口由业务侧提供 Bean：
 * <ul>
 *   <li>开发期：erp-web 的 InMemoryUserDetailsLoader（dev profile）</li>
 *   <li>B1.2 之后：erp-user 模块基于 MySQL 实现</li>
 * </ul>
 */
public interface UserDetailsLoader {

    /**
     * 根据登录名加载用户。
     *
     * @param username 登录名
     * @return 登录用户视图
     * @throws UsernameNotFoundException 用户不存在
     */
    LoginUser loadByUsername(String username) throws UsernameNotFoundException;

    /**
     * 更新指定用户的密码（B1.4 Phase 2）。
     *
     * <p>{@code encryptedPassword} 必须是已 BCrypt 加密后的串，由调用方负责加密。
     * 默认抛 {@link UnsupportedOperationException}：开发期 InMemoryUserDetailsLoader 重写之；
     * MySQL 实现（B1.5 erp-user 模块）后再覆盖即可。
     */
    default void updatePassword(String username, String encryptedPassword) {
        throw new UnsupportedOperationException(
                "当前 UserDetailsLoader 实现未支持 updatePassword：" + getClass().getName());
    }
}
