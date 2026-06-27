package com.erp.data.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 导入任务状态/结果 VO。
 */
@Data
public class ImportTaskVO {

    private String taskId;
    private String fileName;
    private String status;
    private String message;
    private Long totalRows;
    private Long processedRows;
    private Long successCount;
    private Long failCount;
    private Integer percent;
    private List<FailItem> failList;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailItem {
        private int index;
        private String name;
        private String reason;
    }

    public enum Status {
        PENDING, RUNNING, DONE, FAILED
    }
}
