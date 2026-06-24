package com.erp.security.user;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * 用户信息加载器抽象（B1.4 Phase 1）。
 *
 * <p>erp-security 自身不依赖 MySQL/erp-user。本接口由业务侧提供 Bean：
 * <ul>
 *   <li>开发期：erp-web 的 InMemoryUserDetailsLoader（dev profile）</li>
 *   <li>B1.5 起：erp-user 模块 MysqlUserDetailsLoader（带 @Primary，生产路径）</li>
 * </ul>
 */
public interface UserDetailsLoader {

    /**
     * 根据登录名加载用户。
     *
     * @param username 登录名
     * @return 登录用户视图
     * @throws UsernameNotFoundException 用户不存在 / 账号被禁 / 账号锁定中
     */
    LoginUser loadByUsername(String username) throws UsernameNotFoundException;

    /**
     * 更新指定用户的密码（B1.4 Phase 2）。
     *
     * <p>{@code encryptedPassword} 必须是已 BCrypt 加密后的串，由调用方负责加密。
     */
    default void updatePassword(String username, String encryptedPassword) {
        throw new UnsupportedOperationException(
                "当前 UserDetailsLoader 实现未支持 updatePassword：" + getClass().getName());
    }

    /**
     * 登录成功回调（B1.5）：清零失败计数，记录登录时间/IP。
     *
     * <p>默认 no-op，InMemory 实现不需要持久化。MySQL 实现会真正写库。
     *
     * @param username 登录名
     * @param ip       客户端 IP
     */
    default void onLoginSuccess(String username, String ip) {
        // no-op default
    }

    /**
     * 登录失败回调（B1.5）：累计失败次数，达阈值触发账号锁定。
     *
     * <p>默认 no-op。MySQL 实现：连续 5 次失败 → lockedUntil = now+15min。
     *
     * @param username 登录名
     * @return 当前累计失败次数（达到阈值后即锁定）
     */
    default int onLoginFailure(String username) {
        return 0;
    }
}
