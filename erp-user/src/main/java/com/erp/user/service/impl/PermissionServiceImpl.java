package com.erp.user.service.impl;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.user.dto.PermissionCreateRequest;
import com.erp.user.dto.PermissionTreeNode;
import com.erp.user.dto.PermissionUpdateRequest;
import com.erp.user.dto.PermissionVO;
import com.erp.user.entity.SysPermission;
import com.erp.user.mapper.SysDepartmentPermissionMapper;
import com.erp.user.mapper.SysPermissionMapper;
import com.erp.user.mapper.SysRolePermissionMapper;
import com.erp.user.service.PermissionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "erp.user.persistence", havingValue = "mysql", matchIfMissing = true)
public class PermissionServiceImpl implements PermissionService {
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermMapper;
    private final SysDepartmentPermissionMapper deptPermMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PERM_TREE_CACHE_KEY = "erp:user:permission:tree";
    private static final String PERM_LIST_CACHE_KEY = "erp:user:permission:list";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Override
    public List<PermissionTreeNode> treeAll() {
        String cached = redisTemplate.opsForValue().get(PERM_TREE_CACHE_KEY);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<PermissionTreeNode>>() {});
            } catch (Exception e) {
                log.warn("Parse permission tree cache failed, rebuild", e);
            }
        }
        List<PermissionTreeNode> result = buildTree(permissionMapper.selectList(null));
        try {
            redisTemplate.opsForValue().set(PERM_TREE_CACHE_KEY, objectMapper.writeValueAsString(result), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Write permission tree cache failed", e);
        }
        return result;
    }

    @Override
    public List<PermissionVO> listAll() {
        String cached = redisTemplate.opsForValue().get(PERM_LIST_CACHE_KEY);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<PermissionVO>>() {});
            } catch (Exception e) {
                log.warn("Parse permission list cache failed, rebuild", e);
            }
        }
        List<PermissionVO> result = permissionMapper.selectList(null).stream().map(this::toVO).collect(Collectors.toList());
        try {
            redisTemplate.opsForValue().set(PERM_LIST_CACHE_KEY, objectMapper.writeValueAsString(result), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Write permission list cache failed", e);
        }
        return result;
    }
    @Override public PermissionVO getById(Long id) {
        SysPermission p = permissionMapper.selectById(id);
        if (p == null) throw new BusinessException(R.CODE_NOT_FOUND, "permission not found");
        return toVO(p);
    }
    private PermissionVO toVO(SysPermission p) {
        PermissionVO vo = new PermissionVO(); vo.setId(p.getId()); vo.setName(p.getName()); vo.setCode(p.getCode());
        vo.setType(p.getType()); vo.setParentId(p.getParentId()); vo.setSortOrder(p.getSortOrder());
        vo.setPath(p.getPath()); vo.setIcon(p.getIcon()); vo.setStatus(p.getStatus()); vo.setCreatedAt(p.getCreatedAt());
        return vo;
    }
    private List<PermissionTreeNode> buildTree(List<SysPermission> all) {
        Map<Long, List<PermissionTreeNode>> cm = new HashMap<>(); List<PermissionTreeNode> roots = new ArrayList<>();
        for (SysPermission p : all) {
            PermissionTreeNode n = new PermissionTreeNode(); n.setId(p.getId()); n.setName(p.getName());
            n.setCode(p.getCode()); n.setType(p.getType()); n.setParentId(p.getParentId());
            n.setSortOrder(p.getSortOrder()); n.setChildren(new ArrayList<>());
            if (p.getParentId() == null || p.getParentId() == 0) roots.add(n);
            else cm.computeIfAbsent(p.getParentId(), k -> new ArrayList<>()).add(n);
        }
        for (PermissionTreeNode r : roots) buildTreeNode(r, cm);
        return roots;
    }
    private void buildTreeNode(PermissionTreeNode n, Map<Long, List<PermissionTreeNode>> cm) {
        List<PermissionTreeNode> children = cm.get(n.getId());
        if (children != null) { n.getChildren().addAll(children); for (PermissionTreeNode c : children) buildTreeNode(c, cm); }
    }
    @Override @Transactional(rollbackFor = Exception.class) public Long create(PermissionCreateRequest req) {
        SysPermission p = new SysPermission(); p.setName(req.getName()); p.setCode(req.getCode());
        p.setType(req.getType()); p.setParentId(req.getParentId()); p.setSortOrder(req.getSortOrder());
        p.setPath(req.getPath()); p.setIcon(req.getIcon()); p.setStatus(1);
        permissionMapper.insert(p);
        clearCache();
        return p.getId();
    }
    @Override @Transactional(rollbackFor = Exception.class) public void update(Long id, PermissionUpdateRequest req) {
        SysPermission p = permissionMapper.selectById(id);
        if (p == null) throw new BusinessException(R.CODE_NOT_FOUND, "permission not found");
        if (req.getName() != null) p.setName(req.getName());
        if (req.getSortOrder() != null) p.setSortOrder(req.getSortOrder());
        if (req.getPath() != null) p.setPath(req.getPath());
        if (req.getIcon() != null) p.setIcon(req.getIcon());
        permissionMapper.updateById(p);
        clearCache();
    }
    @Override @Transactional(rollbackFor = Exception.class) public void delete(Long id) {
        if (permissionMapper.selectById(id) == null) throw new BusinessException(R.CODE_NOT_FOUND, "permission not found");
        permissionMapper.deleteById(id);
        // 联动删除角色-权限、部门-权限关联
        rolePermMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.erp.user.entity.SysRolePermission>()
                .eq(com.erp.user.entity.SysRolePermission::getPermissionId, id));
        deptPermMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.erp.user.entity.SysDepartmentPermission>()
                .eq(com.erp.user.entity.SysDepartmentPermission::getPermissionId, id));
        clearCache();
    }

    private void clearCache() {
        redisTemplate.delete(PERM_TREE_CACHE_KEY);
        redisTemplate.delete(PERM_LIST_CACHE_KEY);
    }
}