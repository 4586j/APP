package com.erp.finance.service;
import com.erp.finance.dto.*;
public interface AppApprovalService {
    AppApprovalPageVO listPage(AppApprovalRequestQuery q);
    AppApprovalRequestVO getById(Long id);
    /** 创建审批请求（按金额自动匹配流程节点） */
    Long createRequest(String targetType, Long targetId, String title, java.math.BigDecimal amount, Long applicant);
    void approve(Long id, Long approver, String comment);
    void reject(Long id, Long approver, String comment);
}