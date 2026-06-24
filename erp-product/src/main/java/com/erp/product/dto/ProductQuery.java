package com.erp.product.dto;
import lombok.Data;
@Data
public class ProductQuery {
    String keyword; Long categoryId; String hsCode; Integer page = 1; Integer size = 10;
}