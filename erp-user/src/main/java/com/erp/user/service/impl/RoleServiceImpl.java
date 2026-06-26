package com.erp.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.user.dto.RoleCreateRequest;
import com.erp.user.dto.RoleUpdateRequest;
import com.erp.user.dto.RoleVO;
import com.erp.user.entity.SysRole;
import com.erp.user.entity.SysRolePermission;
import com.erp.user.entity.SysUserRole;
import com.erp.user.mapper.SysRoleMapper;
import com.erp.user.mapper.SysRolePermissionMapper;
import com.erp.user.mapper.SysUserRoleMapper;
import com.erp.user.service.RoleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "erp.user.persistence", havingValue = "mysql", matchIfMissing = true)
public class RoleServiceImpl implements RoleService {
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String ROLE_LIST_CACHE_KEY = "erp:user:role:list";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Override
    public List<RoleVO> listAll() {
        String cached = redisTemplate.opsForValue().get(ROLE_LIST_CACHE_KEY);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<RoleVO>>() {});
            } catch (Exception e) {
                log.warn("Parse role list cache failed, rebuild", e);
            }
        }
        List<RoleVO> result = roleMapper.selectList(null).stream().map(this::toVO).collect(Collectors.toList());
        try {
            redisTemplate.opsForValue().set(ROLE_LIST_CACHE_KEY, objectMapper.writeValueAsString(result), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Write role list cache failed", e);
        }
        return result;
    }

    @Override
    public RoleVO getById(Long id) {
        SysRole r = roleMapper.selectById(id);
        if (r == null) {
            throw new BusinessException(R.CODE_NOT_FOUND, "role not found");
        }
        return toVO(r);
    }

    private RoleVO toVO(SysRole r) {
        return RoleVO.builder()
            .id(r.getId())
            .roleName(r.getRoleName())
            .roleCode(r.getRoleCode())
            .dataScope(r.getDataScope())
            .description(r.getDescription())
            .status(r.getStatus())
            .createdAt(r.getCreatedAt())
            .permissionIds(permissionIds(r.getId()))
            .build();
    }

    private List<Long> permissionIds(Long roleId) {
        return rolePermissionMapper.selectList(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, roleId))
            .stream()
            .map(SysRolePermission::getPermissionId)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RoleCreateRequest req) {
        SysRole r = new SysRole();
        r.setRoleName(req.getRoleName());
        r.setRoleCode(req.getRoleCode());
        r.setDataScope(req.getDataScope());
        r.setDescription(req.getDescription());
        r.setStatus(1);
        roleMapper.insert(r);
        clearCache();
        return r.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RoleUpdateRequest req) {
        SysRole r = roleMapper.selectById(id);
        if (r == null) {
            throw new BusinessException(R.CODE_NOT_FOUND, "role not found");
        }
        if (req.getRoleName() != null) {
            r.setRoleName(req.getRoleName());
        }
        if (req.getDescription() != null) {
            r.setDescription(req.getDescription());
        }
        if (req.getDataScope() != null) {
            r.setDataScope(req.getDataScope());
        }
        roleMapper.updateById(r);
        clearCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (roleMapper.selectById(id) == null) {
            throw new BusinessException(R.CODE_NOT_FOUND, "role not found");
        }
        roleMapper.deleteById(id);
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        clearCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUsers(Long roleId, List<Long> userIds) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException(R.CODE_NOT_FOUND, "role not found");
        }
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
        if (userIds != null && !userIds.isEmpty()) {
            for (Long uid : userIds) {
                SysUserRole ur = new SysUserRole();
                ur.setRoleId(roleId);
                ur.setUserId(uid);
                userRoleMapper.insert(ur);
            }
        }
        clearCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException(R.CODE_NOT_FOUND, "role not found");
        }
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long pid : permissionIds) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(pid);
                rolePermissionMapper.insert(rp);
            }
        }
        clearCache();
    }

    private void clearCache() {
        redisTemplate.delete(ROLE_LIST_CACHE_KEY);
    }
}
