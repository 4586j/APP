package com.erp.finance.dto;
import lombok.Data;
@Data
public class AppApprovalRequestQuery {
    String targetType; String status; Long applicant; Integer page=1; Integer size=20;
}