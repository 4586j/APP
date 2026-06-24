package com.erp.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 更新角色请求。roleCode 不可修改。 */
@Data
public class RoleUpdateRequest {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64)
    private String roleName;

    private Integer dataScope;

    @Size(max = 256)
    private String description;

    private Integer status;
}
