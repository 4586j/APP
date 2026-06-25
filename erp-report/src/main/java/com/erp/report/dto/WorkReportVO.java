package com.erp.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 合并工作报表视图（一行同时展示计划和日志）。
 */
@Data
@Builder
public class WorkReportVO {

    private Long userId;
    private String username;
    private String realName;
    private Long departmentId;
    private String departmentName;
    private LocalDate reportDate;

    // 工作计划
    private Long planId;
    private String planTitle;
    private String planContent;
    private String planStatus;
    private LocalDateTime planCreatedAt;
    private LocalDateTime planApproveTime;
    private String planApproveComment;

    // 工作日志
    private Long logId;
    private String logTitle;
    private String logContent;
    private String logStatus;
    private LocalDateTime logCreatedAt;
    private LocalDateTime logApproveTime;
    private String logApproveComment;
}
