package com.erp.report.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 工作报表统计（工作台用）。
 */
@Data
@Builder
public class WorkReportStatsVO {

    private long planCount;
    private long logCount;
    private long rejectedCount;
}
