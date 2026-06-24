package com.erp.order.service;
import com.erp.order.dto.*;
public interface OrdPurchaseOrderService {
    PurchaseOrderPageVO listPage(PurchaseOrderQuery q);
    PurchaseOrderVO getById(Long id);
    PurchaseOrderVO create(PurchaseOrderCreateRequest req, Long userId);
    void updateStatus(Long id, StatusChangeRequest req, Long userId);
    void delete(Long id);
}