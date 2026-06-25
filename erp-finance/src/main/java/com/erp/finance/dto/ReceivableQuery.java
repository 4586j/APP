package com.erp.finance.dto;
import lombok.Data;
@Data
public class ReceivableQuery {
    String status; Long customerId; String keyword; Integer page=1; Integer size=20;
}