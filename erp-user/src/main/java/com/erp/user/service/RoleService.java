package com.erp.user.service;
import com.erp.user.dto.*;
import java.util.List;

public interface RoleService {
    List<RoleVO> listAll();
    RoleVO getById(Long id);
    Long create(RoleCreateRequest req);
    void update(Long id, RoleUpdateRequest req);
    void delete(Long id);
    void assignPermissions(Long roleId, List<Long> permissionIds);
}