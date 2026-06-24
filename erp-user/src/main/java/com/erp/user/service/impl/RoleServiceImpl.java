package com.erp.user.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.user.dto.*;
import com.erp.user.entity.*;
import com.erp.user.mapper.*;
import com.erp.user.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "erp.user.persistence", havingValue = "mysql", matchIfMissing = true)
public class RoleServiceImpl implements RoleService {
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    @Override public List<RoleVO> listAll() {
        return roleMapper.selectList(null).stream().map(r -> RoleVO.builder()
            .id(r.getId()).roleName(r.getRoleName()).roleCode(r.getRoleCode())
            .description(r.getDescription()).status(r.getStatus()).createdAt(r.getCreatedAt()).build()
        ).collect(Collectors.toList());
    }
    @Override public RoleVO getById(Long id) {
        SysRole r = roleMapper.selectById(id);
        if (r == null) throw new BusinessException(R.CODE_NOT_FOUND, "role not found");
        return RoleVO.builder().id(r.getId()).roleName(r.getRoleName()).roleCode(r.getRoleCode())
            .description(r.getDescription()).status(r.getStatus()).createdAt(r.getCreatedAt()).build();
    }
    @Override @Transactional(rollbackFor = Exception.class) public Long create(RoleCreateRequest req) {
        SysRole r = new SysRole(); r.setRoleName(req.getRoleName()); r.setRoleCode(req.getRoleCode());
        r.setDescription(req.getDescription()); r.setStatus(1); roleMapper.insert(r); return r.getId();
    }
    @Override @Transactional(rollbackFor = Exception.class) public void update(Long id, RoleUpdateRequest req) {
        SysRole r = roleMapper.selectById(id);
        if (r == null) throw new BusinessException(R.CODE_NOT_FOUND, "role not found");
        if (req.getRoleName() != null) r.setRoleName(req.getRoleName());
        if (req.getDescription() != null) r.setDescription(req.getDescription());
        roleMapper.updateById(r);
    }
    @Override @Transactional(rollbackFor = Exception.class) public void delete(Long id) {
        if (roleMapper.selectById(id) == null) throw new BusinessException(R.CODE_NOT_FOUND, "role not found");
        roleMapper.deleteById(id);
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
    }
    @Override @Transactional(rollbackFor = Exception.class) public void assignPermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        if (permissionIds != null && !permissionIds.isEmpty())
            for (Long pid : permissionIds) { SysRolePermission rp = new SysRolePermission(); rp.setRoleId(roleId); rp.setPermissionId(pid); rolePermissionMapper.insert(rp); }
    }
}