package com.erp.finance.service;
import com.erp.finance.dto.*;
public interface FinFundApprovalService {
    FundApprovalPageVO listPage(FundApprovalQuery q);
    Long create(FundApprovalCreateRequest r, Long userId);
    void approve(Long id, Long userId, String comment);
    void reject(Long id, String comment, Long userId);
    /** 我的待审批：按审批角色过滤 status=pending（role 为空则返回全部待审） */
    FundApprovalPageVO myPending(String approverRole, Integer page, Integer size);

    /** 金额阈值 → 审批角色：<1万 部门经理 / <10万 财务总监 / >=10万 总经理 */
    static String resolveApproverRole(java.math.BigDecimal amount){
        if(amount==null) return "dept_manager";
        if(amount.compareTo(new java.math.BigDecimal("10000"))<0) return "dept_manager";
        if(amount.compareTo(new java.math.BigDecimal("100000"))<0) return "finance_director";
        return "general_manager";
    }
}