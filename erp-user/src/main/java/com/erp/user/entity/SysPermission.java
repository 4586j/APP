package com.erp.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long parentId;

    @TableField("perm_name")
    private String name;

    @TableField("perm_code")
    private String code;

    @TableField("perm_type")
    private String type;

    private String httpMethod;
    private String icon;
    private String path;
    private String component;
    private Integer sortOrder;

    @TableField("visible")
    private Integer status;
}
