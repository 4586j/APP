package com.erp.customer.dto;
import lombok.Data;
@Data
public class CustomerQuery {
    String keyword; String country; String customerType;
    Integer page = 1; Integer size = 10;
}