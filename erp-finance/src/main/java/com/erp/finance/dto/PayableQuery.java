package com.erp.finance.dto;
import lombok.Data;
@Data
public class PayableQuery {
    String status; Long supplierId; String keyword; Integer page=1; Integer size=20;
}