package com.erp.user.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.user.entity.SysDeptUserPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysDeptUserPermissionMapper extends BaseMapper<SysDeptUserPermission> {

    /**
     * 查询用户在其部门下被授予的权限编码列表。
     */
    @Select("SELECT DISTINCT p.perm_code FROM sys_permission p " +
            "INNER JOIN sys_dept_user_permission dup ON p.id = dup.permission_id " +
            "WHERE dup.user_id = #{userId} AND dup.dept_id = #{deptId}")
    List<String> selectPermissionCodesByUserAndDept(Long userId, Long deptId);

    /**
     * 查询用户在所在所有部门下被授予的权限编码列表。
     */
    @Select("SELECT DISTINCT p.perm_code FROM sys_permission p " +
            "INNER JOIN sys_dept_user_permission dup ON p.id = dup.permission_id " +
            "WHERE dup.user_id = #{userId}")
    List<String> selectPermissionCodesByUserId(Long userId);

    /**
     * 查询某部门下某个用户已被授予的权限ID列表。
     */
    @Select("SELECT permission_id FROM sys_dept_user_permission WHERE user_id = #{userId} AND dept_id = #{deptId}")
    List<Long> selectPermissionIdsByUserAndDept(Long userId, Long deptId);

    /**
     * 删除某部门下某个用户的所有权限分配。
     */
    default int deleteByUserAndDept(Long userId, Long deptId) {
        return delete(new LambdaQueryWrapper<SysDeptUserPermission>()
                .eq(SysDeptUserPermission::getUserId, userId)
                .eq(SysDeptUserPermission::getDeptId, deptId));
    }
}
