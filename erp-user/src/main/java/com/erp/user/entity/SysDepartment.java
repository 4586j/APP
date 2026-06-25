package com.erp.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_department")
public class SysDepartment extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long parentId;

    @TableField("dept_code")
    private String code;

    @TableField("dept_name")
    private String name;

    private String deptPath;
    private Integer sortOrder;
    private Integer status;
}
