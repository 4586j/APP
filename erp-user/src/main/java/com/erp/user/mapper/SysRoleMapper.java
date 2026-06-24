package com.erp.user.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.user.entity.SysRole;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface SysRoleMapper extends BaseMapper<SysRole> {
    @Select("SELECT r.role_code FROM sys_role r INNER JOIN sys_user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    List<String> selectRolesByUserId(Long userId);
}
