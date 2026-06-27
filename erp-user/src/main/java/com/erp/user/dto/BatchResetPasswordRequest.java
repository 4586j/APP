package com.erp.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理员批量重置用户密码。
 */
@Data
public class BatchResetPasswordRequest {

    @NotEmpty(message = "请选择要重置密码的用户")
    private List<Long> userIds;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "新密码长度 8-64")
    private String newPassword;
}
