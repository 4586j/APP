package com.erp.customer.dto;
import lombok.Data;
@Data
public class SupplierQuery {
    String keyword; String province; Integer rating;
    Integer page = 1; Integer size = 10;
}