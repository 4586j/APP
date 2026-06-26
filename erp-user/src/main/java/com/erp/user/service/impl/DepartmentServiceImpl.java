package com.erp.user.service.impl;

import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.user.dto.DepartmentCreateRequest;
import com.erp.user.dto.DepartmentOption;
import com.erp.user.dto.DepartmentTreeNode;
import com.erp.user.dto.DepartmentUpdateRequest;
import com.erp.user.entity.SysDepartment;
import com.erp.user.entity.SysDepartmentPermission;
import com.erp.user.entity.SysUser;
import com.erp.user.mapper.SysDepartmentMapper;
import com.erp.user.mapper.SysDepartmentPermissionMapper;
import com.erp.user.mapper.SysUserMapper;
import com.erp.user.service.DepartmentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "erp.user.persistence", havingValue = "mysql", matchIfMissing = true)
public class DepartmentServiceImpl implements DepartmentService {

    private final SysDepartmentMapper departmentMapper;
    private final SysUserMapper userMapper;
    private final SysDepartmentPermissionMapper deptPermMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String DEPT_OPTIONS_CACHE_KEY = "erp:user:department:options";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    @Override
    public List<DepartmentTreeNode> treeAll() {
        List<SysDepartment> all = departmentMapper.selectList(null);
        Map<Long, List<DepartmentTreeNode>> cm = new HashMap<>();
        List<DepartmentTreeNode> roots = new ArrayList<>();
        for (SysDepartment d : all) {
            DepartmentTreeNode node = new DepartmentTreeNode();
            node.setId(d.getId());
            node.setName(d.getName());
            node.setCode(d.getCode());
            node.setParentId(d.getParentId());
            node.setSortOrder(d.getSortOrder());
            node.setDeptPath(d.getDeptPath());
            node.setStatus(d.getStatus());
            node.setChildren(new ArrayList<>());
            if (d.getParentId() == null || d.getParentId() == 0) roots.add(node);
            else cm.computeIfAbsent(d.getParentId(), k -> new ArrayList<>()).add(node);
        }
        for (DepartmentTreeNode root : roots) buildTree(root, cm);
        return roots;
    }

    private void buildTree(DepartmentTreeNode node, Map<Long, List<DepartmentTreeNode>> cm) {
        List<DepartmentTreeNode> children = cm.get(node.getId());
        if (children != null) {
            node.getChildren().addAll(children);
            for (DepartmentTreeNode c : children) buildTree(c, cm);
        }
    }

    @Override
    public DepartmentTreeNode getById(Long id) {
        SysDepartment d = departmentMapper.selectById(id);
        if (d == null) throw new BusinessException(R.CODE_NOT_FOUND, "department not found");
        DepartmentTreeNode node = new DepartmentTreeNode();
        node.setId(d.getId());
        node.setName(d.getName());
        node.setCode(d.getCode());
        node.setParentId(d.getParentId());
        node.setSortOrder(d.getSortOrder());
        node.setDeptPath(d.getDeptPath());
        node.setStatus(d.getStatus());
        return node;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DepartmentCreateRequest req) {
        SysDepartment d = new SysDepartment();
        d.setName(req.getName());
        d.setCode(req.getCode());
        d.setParentId(req.getParentId());
        d.setSortOrder(req.getSortOrder());
        d.setStatus(req.getStatus());
        departmentMapper.insert(d);
        clearOptionsCache();
        return d.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DepartmentUpdateRequest req) {
        SysDepartment d = departmentMapper.selectById(id);
        if (d == null) throw new BusinessException(R.CODE_NOT_FOUND, "department not found");
        if (req.getName() != null) d.setName(req.getName());
        if (req.getCode() != null) d.setCode(req.getCode());
        if (req.getParentId() != null) d.setParentId(req.getParentId());
        if (req.getSortOrder() != null) d.setSortOrder(req.getSortOrder());
        if (req.getStatus() != null) d.setStatus(req.getStatus());
        departmentMapper.updateById(d);
        clearOptionsCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (departmentMapper.selectById(id) == null)
            throw new BusinessException(R.CODE_NOT_FOUND, "department not found");
        // 校验是否有子部门
        long childCount = departmentMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysDepartment>()
                        .eq(SysDepartment::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(R.CODE_PARAM_INVALID, "请先删除子部门或移除用户");
        }
        // 校验是否有用户归属
        long userCount = userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getDepartmentId, id));
        if (userCount > 0) {
            throw new BusinessException(R.CODE_PARAM_INVALID, "请先删除子部门或移除用户");
        }
        departmentMapper.deleteById(id);
        clearOptionsCache();
    }

    @Override
    public List<DepartmentOption> selectOptions(String keyword) {
        // 优先读缓存（仅无 keyword 时缓存）
        if (!StringUtils.hasText(keyword)) {
            String cached = redisTemplate.opsForValue().get(DEPT_OPTIONS_CACHE_KEY);
            if (cached != null) {
                try {
                    return objectMapper.readValue(cached, new TypeReference<List<DepartmentOption>>() {});
                } catch (Exception e) {
                    log.warn("Parse department options cache failed, rebuild", e);
                }
            }
        }

        List<SysDepartment> all = departmentMapper.selectList(null);
        List<DepartmentOption> result = buildOptionTree(all, keyword);

        if (!StringUtils.hasText(keyword)) {
            try {
                String json = objectMapper.writeValueAsString(result);
                redisTemplate.opsForValue().set(DEPT_OPTIONS_CACHE_KEY, json, CACHE_TTL);
            } catch (Exception e) {
                log.warn("Write department options cache failed", e);
            }
        }
        return result;
    }

    private List<DepartmentOption> buildOptionTree(List<SysDepartment> all, String keyword) {
        // 如果有 keyword，先过滤匹配项及其祖先
        Set<Long> matchedIds = new HashSet<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim().toLowerCase();
            Map<Long, SysDepartment> idMap = all.stream().collect(Collectors.toMap(SysDepartment::getId, d -> d));
            for (SysDepartment d : all) {
                if (d.getName() != null && d.getName().toLowerCase().contains(kw)) {
                    matchedIds.add(d.getId());
                    // 添加所有祖先
                    Long pid = d.getParentId();
                    while (pid != null && pid != 0 && idMap.containsKey(pid)) {
                        matchedIds.add(pid);
                        pid = idMap.get(pid).getParentId();
                    }
                }
            }
        }

        Map<Long, List<DepartmentOption>> cm = new HashMap<>();
        List<DepartmentOption> roots = new ArrayList<>();
        for (SysDepartment d : all) {
            if (!matchedIds.isEmpty() && !matchedIds.contains(d.getId())) continue;
            DepartmentOption opt = new DepartmentOption();
            opt.setId(d.getId());
            opt.setName(d.getName());
            opt.setCode(d.getCode());
            opt.setParentId(d.getParentId());
            opt.setSortOrder(d.getSortOrder());
            opt.setStatus(d.getStatus());
            if (d.getParentId() == null || d.getParentId() == 0) roots.add(opt);
            else cm.computeIfAbsent(d.getParentId(), k -> new ArrayList<>()).add(opt);
        }
        for (DepartmentOption root : roots) buildOptionTree(root, cm);
        return roots;
    }

    private void buildOptionTree(DepartmentOption node, Map<Long, List<DepartmentOption>> cm) {
        List<DepartmentOption> children = cm.get(node.getId());
        if (children != null) {
            node.getChildren().addAll(children);
            for (DepartmentOption c : children) buildOptionTree(c, cm);
        }
    }

    @Override
    public List<Long> getPermissionIdsByDeptId(Long deptId) {
        return deptPermMapper.selectPermissionIdsByDeptId(deptId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long deptId, List<Long> permissionIds) {
        if (departmentMapper.selectById(deptId) == null) {
            throw new BusinessException(R.CODE_NOT_FOUND, "department not found");
        }
        deptPermMapper.deleteByDepartmentId(deptId);
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long pid : permissionIds) {
                SysDepartmentPermission dp = new SysDepartmentPermission();
                dp.setDepartmentId(deptId);
                dp.setPermissionId(pid);
                deptPermMapper.insert(dp);
            }
        }
        clearOptionsCache();
    }

    private void clearOptionsCache() {
        redisTemplate.delete(DEPT_OPTIONS_CACHE_KEY);
    }
}
