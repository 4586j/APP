package com.erp.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理员重置用户密码请求。重置后强制下次登录改密。 */
@Data
public class ResetPasswordRequest {

    @NotBlank
    @Size(min = 8, max = 64, message = "新密码长度 8-64")
    private String newPassword;
}
