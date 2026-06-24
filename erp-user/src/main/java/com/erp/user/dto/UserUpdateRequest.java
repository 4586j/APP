package com.erp.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 更新用户请求。username 与 password 不通过本接口修改。 */
@Data
public class UserUpdateRequest {

    @Size(max = 64)
    private String realName;

    @Email(message = "邮箱格式错误")
    @Size(max = 128)
    private String email;

    @Size(max = 32)
    private String phone;

    private String avatarUrl;
    private Long departmentId;
    private Long superiorId;
    private Integer status;
}
