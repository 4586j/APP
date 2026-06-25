package com.erp.report.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 工作报表管理查询。
 */
@Data
public class WorkReportQuery {

    private LocalDate reportDate;
    private Long departmentId;
    private String keyword;
    private String status;
    private String type;
    private Integer page = 1;
    private Integer size = 20;
}
