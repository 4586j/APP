package com.erp.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 创建用户请求。 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "登录名不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{2,31}$",
            message = "登录名 3-32 位，仅字母数字下划线，且首字符为字母")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度 8-64")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 64)
    private String realName;

    @Email(message = "邮箱格式错误")
    @Size(max = 128)
    private String email;

    @Size(max = 32)
    private String phone;

    /** 部门 ID。 */
    private Long departmentId;

    /** 直属上级用户 ID，可空。 */
    private Long superiorId;

    /** 角色 ID 列表，可空（创建时一并绑定）。 */
    private List<Long> roleIds;

    /** 1=启用 0=禁用。 */
    private Integer status = 1;
}
