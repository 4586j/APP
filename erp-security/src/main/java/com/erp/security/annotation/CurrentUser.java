package com.erp.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注入当前登录用户的方法参数注解（B1.4 Phase 1）。
 *
 * <p>由 {@code CurrentUserArgumentResolver} 解析：
 * <ul>
 *   <li>参数类型 {@code String} → 注入用户名</li>
 *   <li>参数类型 {@code com.erp.security.user.LoginUser} → 通过 UserDetailsLoader 重新加载</li>
 * </ul>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
