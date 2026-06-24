package com.erp.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private String username;

    @TableField("password")
    private String passwordHash;

    private String realName;
    private String email;
    private String phone;
    private String avatarUrl;
    private Long departmentId;
    private Long superiorId;
    private Integer status;
    private Integer pwdResetRequired;

    @TableField("failed_login_count")
    private Integer loginFailCount;

    private LocalDateTime lockedUntil;

    @TableField("last_login_at")
    private LocalDateTime lastLoginTime;

    private String lastLoginIp;
}
