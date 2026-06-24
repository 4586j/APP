package com.erp.product.service;

import com.erp.product.dto.PrdCategoryVO;
import com.erp.product.dto.CategoryCreateRequest;

import java.util.List;

public interface PrdCategoryService {
    List<PrdCategoryVO> listTree();
    PrdCategoryVO getById(Long id);
    Long create(CategoryCreateRequest req);
    void update(Long id, CategoryCreateRequest req);
    void delete(Long id);
}
