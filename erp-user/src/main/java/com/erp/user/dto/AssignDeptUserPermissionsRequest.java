package com.erp.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/**
 * 分配部门用户权限请求。
 */
@Data
public class AssignDeptUserPermissionsRequest {
    @NotNull
    private Long userId;
    @NotNull
    private Long deptId;
    private List<Long> permissionIds;
    private Long grantedBy;
}
