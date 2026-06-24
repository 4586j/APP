package com.erp.customer.service;
import com.erp.customer.dto.*;
public interface CustCustomerService {
    CustomerPageVO listPage(CustomerQuery q);
    CustomerVO getById(Long id);
    Long create(CustomerCreateRequest req);
    void update(Long id, CustomerCreateRequest req);
    void delete(Long id);
}