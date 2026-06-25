package com.erp.user.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DepartmentCreateRequest {
    private Long parentId = 0L;
    @NotBlank(message = "部门编码不能为空")
    @Size(max = 32, message = "部门编码长度不能超过 32")
    private String code;
    @NotBlank(message = "部门名称不能为空")
    @Size(max = 64, message = "部门名称长度不能超过 64")
    private String name;
    private Integer sortOrder = 0;
    private Integer status = 1;
}