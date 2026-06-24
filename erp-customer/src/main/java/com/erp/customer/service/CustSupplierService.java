package com.erp.customer.service;
import com.erp.customer.dto.*;
public interface CustSupplierService {
    SupplierPageVO listPage(SupplierQuery q);
    SupplierVO getById(Long id);
    Long create(SupplierCreateRequest req);
    void update(Long id, SupplierCreateRequest req);
    void delete(Long id);
}