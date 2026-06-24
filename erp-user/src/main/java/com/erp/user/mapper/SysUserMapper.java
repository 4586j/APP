package com.erp.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.user.entity.SysUser;

/** SysUserMapper（B1.5）。MP BaseMapper 提供基础 CRUD，复杂查询用 LambdaQueryWrapper。 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 查用户拥有的所有 roleCode（去重）。 */
    @org.apache.ibatis.annotations.Select("""
        SELECT DISTINCT r.role_code
        FROM sys_user_role ur
        JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.status = 1
        WHERE ur.user_id = #{userId}
        """)
    java.util.List<String> selectRoleCodesByUserId(@org.apache.ibatis.annotations.Param("userId") Long userId);

    /** 查用户通过角色聚合得到的全部 permCode（去重）。 */
    @org.apache.ibatis.annotations.Select("""
        SELECT DISTINCT p.perm_code
        FROM sys_user_role ur
        JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.status = 1
        JOIN sys_role_permission rp ON rp.role_id = r.id
        JOIN sys_permission p ON p.id = rp.permission_id AND p.deleted = 0
        WHERE ur.user_id = #{userId}
        """)
    java.util.List<String> selectPermCodesByUserId(@org.apache.ibatis.annotations.Param("userId") Long userId);
}
