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
}
