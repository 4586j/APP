package com.erp.product.service;
import com.erp.product.dto.*;
public interface PrdProductService {
    ProductPageVO listPage(ProductQuery q); ProductVO getById(Long id);
    Long create(ProductCreateRequest req); void update(Long id, ProductCreateRequest req); void delete(Long id);
}