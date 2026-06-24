package com.erp.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.user.entity.SysUserRole;

/** SysUserRoleMapper（B1.5）。MP BaseMapper 提供基础 CRUD，复杂查询用 LambdaQueryWrapper。 */
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /** 物理删除某用户的所有角色绑定（用于 assignRoles 事务）。 */
    @org.apache.ibatis.annotations.Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@org.apache.ibatis.annotations.Param("userId") Long userId);

    /** 统计角色已被多少用户引用（用于角色删除前校验）。 */
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId}")
    long countByRoleId(@org.apache.ibatis.annotations.Param("roleId") Long roleId);
}
