package com.erp.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统角色实体（B1.5）。
 * data_scope: 1=SELF / 2=DEPT / 3=DEPT_AND_CHILDREN / 4=ALL，用于数据权限粒度。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String roleName;
    private String roleCode;
    private Integer dataScope;
    private String description;
    private Integer status;
}
