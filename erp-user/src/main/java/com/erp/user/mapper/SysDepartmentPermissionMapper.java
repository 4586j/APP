package com.erp.user.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.user.entity.SysDepartmentPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysDepartmentPermissionMapper extends BaseMapper<SysDepartmentPermission> {

    @Select("SELECT permission_id FROM sys_department_permission WHERE department_id = #{deptId}")
    List<Long> selectPermissionIdsByDeptId(Long deptId);

    default int deleteByDepartmentId(Long deptId) {
        return delete(new LambdaQueryWrapper<SysDepartmentPermission>()
                .eq(SysDepartmentPermission::getDepartmentId, deptId));
    }
}
