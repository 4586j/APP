package com.erp.user.service;
import com.erp.user.dto.*;
import java.util.List;

public interface PermissionService {
    List<PermissionTreeNode> treeAll();
    List<PermissionVO> listAll();
    PermissionVO getById(Long id);
    Long create(PermissionCreateRequest req);
    void update(Long id, PermissionUpdateRequest req);
    void delete(Long id);
}