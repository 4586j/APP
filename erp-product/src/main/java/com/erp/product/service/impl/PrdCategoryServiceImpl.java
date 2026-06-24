package com.erp.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.product.dto.PrdCategoryVO;
import com.erp.product.dto.CategoryCreateRequest;
import com.erp.product.entity.PrdCategory;
import com.erp.product.mapper.PrdCategoryMapper;
import com.erp.product.service.PrdCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrdCategoryServiceImpl implements PrdCategoryService {
    final PrdCategoryMapper categoryMapper;

    @Override
    public List<PrdCategoryVO> listTree() {
        var all = categoryMapper.selectList(new LambdaQueryWrapper<PrdCategory>()
                .orderByAsc(PrdCategory::getSortOrder)
                .orderByDesc(PrdCategory::getCreatedAt));
        if (all.isEmpty()) return List.of();
        var voMap = all.stream().collect(Collectors.toMap(PrdCategory::getId, this::toVO));
        List<PrdCategoryVO> roots = new ArrayList<>();
        for (var c : all) {
            var vo = voMap.get(c.getId());
            if (c.getParentId() == null || c.getParentId() == 0) {
                roots.add(vo);
            } else {
                var parent = voMap.get(c.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) parent.setChildren(new ArrayList<>());
                    parent.getChildren().add(vo);
                } else {
                    roots.add(vo);
                }
            }
        }
        return roots;
    }

    PrdCategoryVO toVO(PrdCategory x) {
        var vo = PrdCategoryVO.builder()
                .id(x.getId()).parentId(x.getParentId()).catName(x.getCatName())
                .catCode(x.getCatCode()).sortOrder(x.getSortOrder())
                .createdAt(x.getCreatedAt())
                .build();
        return vo;
    }

    @Override
    public PrdCategoryVO getById(Long id) {
        var x = categoryMapper.selectById(id);
        if (x == null) throw new BusinessException(R.CODE_NOT_FOUND, "category not found");
        return toVO(x);
    }

    @Override
    @Transactional
    public Long create(CategoryCreateRequest req) {
        var c = new PrdCategory();
        c.setParentId(req.getParentId() != null ? req.getParentId() : 0);
        c.setCatName(req.getCatName());
        c.setCatCode(req.getCatCode());
        c.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        categoryMapper.insert(c);
        return c.getId();
    }

    @Override
    @Transactional
    public void update(Long id, CategoryCreateRequest req) {
        var c = categoryMapper.selectById(id);
        if (c == null) throw new BusinessException(R.CODE_NOT_FOUND, "category not found");
        if (req.getCatName() != null) c.setCatName(req.getCatName());
        if (req.getCatCode() != null) c.setCatCode(req.getCatCode());
        if (req.getParentId() != null) c.setParentId(req.getParentId());
        if (req.getSortOrder() != null) c.setSortOrder(req.getSortOrder());
        categoryMapper.updateById(c);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 如果有关联子分类则不允许删除
        var count = categoryMapper.selectCount(new LambdaQueryWrapper<PrdCategory>()
                .eq(PrdCategory::getParentId, id));
        if (count > 0) throw new BusinessException(R.CODE_PARAM_INVALID, "请先删除子分类");
        categoryMapper.deleteById(id);
    }
}
