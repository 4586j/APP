package com.erp.user.service.impl;

import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.user.entity.SysDeptUserPermission;
import com.erp.user.entity.SysUser;
import com.erp.user.mapper.SysDeptUserPermissionMapper;
import com.erp.user.mapper.SysDepartmentPermissionMapper;
import com.erp.user.mapper.SysDepartmentMapper;
import com.erp.user.mapper.SysUserMapper;
import com.erp.user.service.DeptUserPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "erp.user.persistence", havingValue = "mysql", matchIfMissing = true)
public class DeptUserPermissionServiceImpl implements DeptUserPermissionService {

    private final SysDeptUserPermissionMapper mapper;
    private final SysDepartmentMapper departmentMapper;
    private final SysDepartmentPermissionMapper deptPermMapper;
    private final SysUserMapper userMapper;

    @Override
    public List<Long> getPermissionIdsByUserAndDept(Long userId, Long deptId) {
        return mapper.selectPermissionIdsByUserAndDept(userId, deptId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long userId, Long deptId, List<Long> permissionIds, Long grantedBy) {
        if (departmentMapper.selectById(deptId) == null) {
            throw new BusinessException(R.CODE_NOT_FOUND, "department not found");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(R.CODE_NOT_FOUND, "user not found");
        }
        if (user.getDepartmentId() == null || !user.getDepartmentId().equals(deptId)) {
            throw new BusinessException(R.CODE_FORBIDDEN, "user does not belong to this department");
        }
        if (permissionIds != null && !permissionIds.isEmpty()) {
            Set<Long> deptPermissionIds = new HashSet<>(deptPermMapper.selectPermissionIdsByDeptId(deptId));
            if (!deptPermissionIds.containsAll(permissionIds)) {
                throw new BusinessException(R.CODE_FORBIDDEN, "permission exceeds department limit");
            }
        }
        // 覆盖式分配：先删后插
        mapper.deleteByUserAndDept(userId, deptId);
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long pid : permissionIds) {
                SysDeptUserPermission dup = new SysDeptUserPermission();
                dup.setUserId(userId);
                dup.setDeptId(deptId);
                dup.setPermissionId(pid);
                dup.setGrantedBy(grantedBy);
                mapper.insert(dup);
            }
        }
        log.info("dept user permission assigned: userId={}, deptId={}, perms={}, grantedBy={}",
                userId, deptId, permissionIds, grantedBy);
    }

    @Override
    public List<String> getPermissionCodesByUserId(Long userId) {
        return mapper.selectPermissionCodesByUserId(userId);
    }
}
