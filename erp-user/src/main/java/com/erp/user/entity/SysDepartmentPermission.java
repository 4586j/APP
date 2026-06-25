package com.erp.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 部门-权限关联实体。
 */
@Data
@TableName("sys_department_permission")
public class SysDepartmentPermission {
    private Long id;
    private Long departmentId;
    private Long permissionId;
}
