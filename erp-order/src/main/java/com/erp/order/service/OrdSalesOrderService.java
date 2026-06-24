package com.erp.order.service;
import com.erp.order.dto.*;
public interface OrdSalesOrderService {
    SalesOrderPageVO listPage(SalesOrderQuery q);
    SalesOrderVO getById(Long id);
    SalesOrderVO create(SalesOrderCreateRequest req, Long userId);
    void update(Long id, SalesOrderUpdateRequest req);
    SalesOrderVO changeStatus(Long id, StatusChangeRequest req, Long userId);
    void delete(Long id);
}