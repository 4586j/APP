package com.erp.report.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 批量审批请求。
 */
@Data
public class BatchApproveRequest {

    private List<Long> planIds;
    private List<Long> logIds;

    @NotBlank(message = "审批动作不能为空")
    private String action;

    private String comment;
}
