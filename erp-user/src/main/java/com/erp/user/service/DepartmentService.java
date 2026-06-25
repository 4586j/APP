package com.erp.user.service;
import com.erp.user.dto.DepartmentCreateRequest;
import com.erp.user.dto.DepartmentTreeNode;
import com.erp.user.dto.DepartmentUpdateRequest;
import java.util.List;

public interface DepartmentService {
    List<DepartmentTreeNode> treeAll();
    DepartmentTreeNode getById(Long id);
    Long create(DepartmentCreateRequest req);
    void update(Long id, DepartmentUpdateRequest req);
    void delete(Long id);

    /**
     * 获取部门下拉选项（树形），支持 keyword 模糊搜索过滤。
     */
    List<com.erp.user.dto.DepartmentOption> selectOptions(String keyword);

    /**
     * 获取部门已分配的权限 ID 列表。
     */
    List<Long> getPermissionIdsByDeptId(Long deptId);

    /**
     * 为部门分配权限（覆盖式）。
     */
    void assignPermissions(Long deptId, List<Long> permissionIds);
}