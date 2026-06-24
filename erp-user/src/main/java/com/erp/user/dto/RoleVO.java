package com.erp.user.dto;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RoleVO {
    private Long id;
    private String roleName;
    private String roleCode;
    private Integer dataScope;
    private String description;
    private Integer status;
    private LocalDateTime createdAt;
    private List<Long> permissionIds;
}