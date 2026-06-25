package com.erp.report.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 工作报表分页结果。
 */
@Data
@Builder
public class WorkReportPageVO {

    private List<WorkReportVO> records;
    private long total;
    private long size;
    private long current;
}
