package com.erp.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PermissionCreateRequest {
    private Long parentId = 0L;
    @NotBlank(message = "权限名称不能为空")
    @Size(max = 64)
    private String name;
    @NotBlank(message = "权限编码不能为空")
    @Size(max = 128)
    private String code;
    @Pattern(regexp = "menu|button|api", message = "permType 仅支持 menu|button|api")
    private String type = "menu";
    private String httpMethod;
    private String icon;
    private String path;
    private String component;
    private Integer sortOrder = 0;
    private Integer status = 1;
}
