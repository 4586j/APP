package com.erp.user.dto;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DepartmentUpdateRequest {
    @Size(max = 64, message = "部门名称长度不能超过 64")
    private String name;
    private String code;
    private Long parentId;
    private Integer sortOrder;
}