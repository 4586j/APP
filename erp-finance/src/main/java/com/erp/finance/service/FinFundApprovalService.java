package com.erp.finance.service;
import com.erp.finance.dto.*;
public interface FinFundApprovalService {
    FundApprovalPageVO listPage(FundApprovalQuery q);
    Long create(FundApprovalCreateRequest r, Long userId);
    void approve(Long id, Long userId);
    void reject(Long id, String comment, Long userId);
}