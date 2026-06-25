package com.erp.user.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.user.entity.SysPermission;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface SysPermissionMapper extends BaseMapper<SysPermission> {
    @Select("SELECT DISTINCT p.perm_code FROM sys_permission p INNER JOIN sys_role_permission rp ON p.id = rp.permission_id INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id WHERE ur.user_id = #{userId}")
    List<String> selectPermissionsByUserId(Long userId);

    /** 通过部门ID查询该部门拥有的权限编码列表。 */
    @Select("SELECT DISTINCT p.perm_code FROM sys_permission p INNER JOIN sys_department_permission dp ON p.id = dp.permission_id WHERE dp.department_id = #{deptId}")
    List<String> selectPermissionsByDeptId(Long deptId);
}
