package com.erp.common.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 全局审计字段自动填充（B1.5）。
 *
 * <p>填充策略：
 * <ul>
 *   <li>createdAt / createdBy：INSERT 时填充</li>
 *   <li>updatedAt / updatedBy：INSERT + UPDATE 时填充</li>
 *   <li>deleted / version：交给 @TableLogic 与 @Version 处理，不在此处赋值</li>
 * </ul>
 *
 * <p>当前用户从 Spring Security 的 SecurityContext 取——为了保持 erp-common 与 erp-security 解耦，
 * 这里使用反射，避免编译期依赖。未登录场景（如 Flyway seed、系统任务）默认填 0L。
 */
@Component
public class GlobalMetaObjectHandler implements MetaObjectHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalMetaObjectHandler.class);

    private static final String CREATED_AT = "createdAt";
    private static final String UPDATED_AT = "updatedAt";
    private static final String CREATED_BY = "createdBy";
    private static final String UPDATED_BY = "updatedBy";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long userId = currentUserIdOrSystem();
        strictInsertFill(metaObject, CREATED_AT, LocalDateTime.class, now);
        strictInsertFill(metaObject, UPDATED_AT, LocalDateTime.class, now);
        strictInsertFill(metaObject, CREATED_BY, Long.class, userId);
        strictInsertFill(metaObject, UPDATED_BY, Long.class, userId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, UPDATED_AT, LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, UPDATED_BY, Long.class, currentUserIdOrSystem());
    }

    /**
     * 反射读取 SecurityContext 中的 principal，避免 erp-common 编译期依赖 spring-security。
     * principal 期望是 Long 类型的 userId（由 JwtAuthenticationFilter 注入）。
     */
    private Long currentUserIdOrSystem() {
        try {
            Class<?> holderClass = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            Object context = holderClass.getMethod("getContext").invoke(null);
            if (context == null) return 0L;
            Object auth = context.getClass().getMethod("getAuthentication").invoke(context);
            if (auth == null) return 0L;
            Boolean authed = (Boolean) auth.getClass().getMethod("isAuthenticated").invoke(auth);
            if (Boolean.FALSE.equals(authed)) return 0L;
            Object principal = auth.getClass().getMethod("getPrincipal").invoke(auth);
            if (principal instanceof Long uid) return uid;
            if (principal instanceof Number num) return num.longValue();
            if (principal instanceof String s) {
                try { return Long.parseLong(s); } catch (NumberFormatException ignore) { /* fallthrough */ }
            }
        } catch (ClassNotFoundException e) {
            // spring-security 不在 classpath（不应该发生，但容错）
            log.trace("SecurityContextHolder 未找到（spring-security 缺失），审计 userId 落 0");
        } catch (Exception e) {
            log.debug("MetaObjectHandler 取当前用户失败：{}", e.getMessage());
        }
        return 0L;
    }
}
