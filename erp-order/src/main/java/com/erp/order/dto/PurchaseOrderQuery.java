package com.erp.order.dto;
import lombok.Data;
@Data
public class PurchaseOrderQuery {
    String keyword; Long supplierId; String status; Integer page = 1; Integer size = 10;
}