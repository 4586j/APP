package com.erp.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 创建角色请求。 */
@Data
public class RoleCreateRequest {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64)
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^ROLE_[A-Z][A-Z0-9_]{0,63}$",
            message = "角色编码必须 ROLE_ 前缀，大写字母数字下划线")
    private String roleCode;

    /** 1=SELF 2=DEPT 3=DEPT_AND_CHILDREN 4=ALL，默认 2。 */
    private Integer dataScope = 2;

    @Size(max = 256)
    private String description;

    private Integer status = 1;
}
