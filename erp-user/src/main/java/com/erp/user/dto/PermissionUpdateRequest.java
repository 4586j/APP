package com.erp.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PermissionUpdateRequest {
    @NotBlank
    @Size(max = 64)
    private String name;
    private String httpMethod;
    private String icon;
    private String path;
    private String component;
    private Integer sortOrder;
    private Integer status;
}
