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
}