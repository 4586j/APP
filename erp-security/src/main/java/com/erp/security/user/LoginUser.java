package com.erp.security.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 登录用户视图模型（B1.4 Phase 1）。
 *
 * <p>由 {@link UserDetailsLoader} 实现类加载，作为 Spring Security
 * {@code Authentication#getPrincipal()} 注入到 SecurityContext。
 *
 * <p>仅承载“登录态所需信息”，不参与 ORM 映射。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID。 */
    private Long id;

    /** 登录名。 */
    private String username;

    /** BCrypt 加密后的密码（仅登录校验过程使用，不返回前端）。 */
    private String encryptedPassword;

    /** 真实姓名。 */
    private String realName;

    /** 部门编码（枚举字符串，例如 MANAGEMENT/SALES）。 */
    private String department;

    /** 部门显示名。 */
    private String departmentName;

    /** 部门ID。 */
    private Long departmentId;

    /** 角色编码列表，例如 ROLE_ADMIN。 */
    private List<String> roles;

    /** 细粒度权限编码列表，例如 order:view。 */
    private List<String> permissions;
}
