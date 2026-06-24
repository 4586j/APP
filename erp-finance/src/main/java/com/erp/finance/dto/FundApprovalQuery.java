package com.erp.finance.dto;
import lombok.Data;
@Data
public class FundApprovalQuery {
    String requestNo; String fundType; String status; Integer page=1; Integer size=20;
}