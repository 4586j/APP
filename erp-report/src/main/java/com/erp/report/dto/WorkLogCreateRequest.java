package com.erp.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 工作日志创建/更新请求。
 */
@Data
public class WorkLogCreateRequest {

    private Long id;

    @NotNull(message = "报告日期不能为空")
    private LocalDate reportDate;

    private String title;

    @NotBlank(message = "日志内容不能为空")
    private String content;
}
