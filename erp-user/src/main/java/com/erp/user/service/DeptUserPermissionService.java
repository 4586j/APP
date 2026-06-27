package com.erp.user.service;

import java.util.List;

/**
 * 部门用户权限服务：部长为部门内用户分配权限。
 */
public interface DeptUserPermissionService {

    /**
     * 获取某部门下某用户的权限 ID 列表。
     */
    List<Long> getPermissionIdsByUserAndDept(Long userId, Long deptId);

    /**
     * 为某部门下的用户分配权限（覆盖式）。
     *
     * @param userId        目标用户ID
     * @param deptId        所属部门ID
     * @param permissionIds 要分配的权限ID列表
     * @param grantedBy     授权人用户ID
     */
    void assignPermissions(Long userId, Long deptId, List<Long> permissionIds, Long grantedBy);

    /**
     * 查询用户在其所有部门下被授予的权限编码列表。
     */
    List<String> getPermissionCodesByUserId(Long userId);
}
