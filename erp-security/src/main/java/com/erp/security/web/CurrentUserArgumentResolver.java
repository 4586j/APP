package com.erp.security.web;

import com.erp.security.annotation.CurrentUser;
import com.erp.security.user.LoginUser;
import com.erp.security.user.UserDetailsLoader;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link CurrentUser} 参数解析器（B1.4 Phase 1）。
 *
 * <p>从 SecurityContext 取 principal（username），按目标参数类型决定如何注入：
 * <ul>
 *   <li>String → 直接给 username</li>
 *   <li>LoginUser → 调 UserDetailsLoader 重新加载（保证拿到最新角色/权限）</li>
 * </ul>
 *
 * <p>未登录返回 null；Controller 上层若要求强制登录，应通过 Security 配置兜住。
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserDetailsLoader userDetailsLoader;

    public CurrentUserArgumentResolver(UserDetailsLoader userDetailsLoader) {
        this.userDetailsLoader = userDetailsLoader;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        Object principal = auth.getPrincipal();
        String username = principal instanceof String s ? s : auth.getName();
        Class<?> type = parameter.getParameterType();
        if (LoginUser.class.isAssignableFrom(type)) {
            return userDetailsLoader.loadByUsername(username);
        }
        if (String.class.isAssignableFrom(type)) {
            return username;
        }
        return null;
    }
}
