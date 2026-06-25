package com.erp.report.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工作计划实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rpt_work_plan")
public class RptWorkPlan extends BaseEntity {

    private Long userId;
    private Long departmentId;
    private LocalDate reportDate;
    private String title;
    private String content;
    private String status;
    private Long approverId;
    private String approveComment;
    private LocalDateTime approveTime;
}
