package com.erp.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门用户权限：部长为部门内的用户分配具体权限。
 * 管理员(ROLE_ADMIN) 不受此表限制。
 */
@Data
@TableName("sys_dept_user_permission")
public class SysDeptUserPermission {
    private Long id;
    private Long userId;
    private Long deptId;
    private Long permissionId;
    private Long grantedBy;
    private LocalDateTime createdAt;
}
