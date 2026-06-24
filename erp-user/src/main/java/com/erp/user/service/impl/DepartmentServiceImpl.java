package com.erp.user.service.impl;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.user.dto.DepartmentCreateRequest;
import com.erp.user.dto.DepartmentTreeNode;
import com.erp.user.dto.DepartmentUpdateRequest;
import com.erp.user.entity.SysDepartment;
import com.erp.user.mapper.SysDepartmentMapper;
import com.erp.user.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(javax.sql.DataSource.class)
public class DepartmentServiceImpl implements DepartmentService {
    private final SysDepartmentMapper departmentMapper;

    @Override public List<DepartmentTreeNode> treeAll() {
        List<SysDepartment> all = departmentMapper.selectList(null);
        Map<Long, List<DepartmentTreeNode>> cm = new HashMap<>();
        List<DepartmentTreeNode> roots = new ArrayList<>();
        for (SysDepartment d : all) {
            DepartmentTreeNode node = new DepartmentTreeNode();
            node.setId(d.getId()); node.setName(d.getName()); node.setCode(d.getCode());
            node.setParentId(d.getParentId()); node.setSortOrder(d.getSortOrder());
            node.setDeptPath(d.getDeptPath()); node.setChildren(new ArrayList<>());
            if (d.getParentId() == null || d.getParentId() == 0) roots.add(node);
            else cm.computeIfAbsent(d.getParentId(), k -> new ArrayList<>()).add(node);
        }
        for (DepartmentTreeNode root : roots) buildTree(root, cm);
        return roots;
    }
    private void buildTree(DepartmentTreeNode node, Map<Long, List<DepartmentTreeNode>> cm) {
        List<DepartmentTreeNode> children = cm.get(node.getId());
        if (children != null) { node.getChildren().addAll(children); for (DepartmentTreeNode c : children) buildTree(c, cm); }
    }
    @Override public DepartmentTreeNode getById(Long id) {
        SysDepartment d = departmentMapper.selectById(id);
        if (d == null) throw new BusinessException(R.CODE_NOT_FOUND, "department not found");
        DepartmentTreeNode node = new DepartmentTreeNode();
        node.setId(d.getId()); node.setName(d.getName()); node.setCode(d.getCode());
        node.setParentId(d.getParentId()); node.setSortOrder(d.getSortOrder()); node.setDeptPath(d.getDeptPath());
        return node;
    }
    @Override @Transactional(rollbackFor = Exception.class) public Long create(DepartmentCreateRequest req) {
        SysDepartment d = new SysDepartment(); d.setName(req.getName()); d.setCode(req.getCode());
        d.setParentId(req.getParentId()); d.setSortOrder(req.getSortOrder());
        departmentMapper.insert(d); return d.getId();
    }
    @Override @Transactional(rollbackFor = Exception.class) public void update(Long id, DepartmentUpdateRequest req) {
        SysDepartment d = departmentMapper.selectById(id);
        if (d == null) throw new BusinessException(R.CODE_NOT_FOUND, "department not found");
        if (req.getName() != null) d.setName(req.getName());
        if (req.getCode() != null) d.setCode(req.getCode());
        if (req.getParentId() != null) d.setParentId(req.getParentId());
        if (req.getSortOrder() != null) d.setSortOrder(req.getSortOrder());
        departmentMapper.updateById(d);
    }
    @Override @Transactional(rollbackFor = Exception.class) public void delete(Long id) {
        if (departmentMapper.selectById(id) == null) throw new BusinessException(R.CODE_NOT_FOUND, "department not found");
        departmentMapper.deleteById(id);
    }
}