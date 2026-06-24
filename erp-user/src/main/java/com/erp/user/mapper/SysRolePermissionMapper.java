package com.erp.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.user.entity.SysRolePermission;

/** SysRolePermissionMapper（B1.5）。MP BaseMapper 提供基础 CRUD，复杂查询用 LambdaQueryWrapper。 */
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /** 物理删除某角色的所有权限绑定（用于 assignPermissions 事务）。 */
    @org.apache.ibatis.annotations.Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@org.apache.ibatis.annotations.Param("roleId") Long roleId);

    /** 统计某权限被多少角色引用（用于权限删除前校验）。 */
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM sys_role_permission WHERE permission_id = #{permissionId}")
    long countByPermissionId(@org.apache.ibatis.annotations.Param("permissionId") Long permissionId);
}
