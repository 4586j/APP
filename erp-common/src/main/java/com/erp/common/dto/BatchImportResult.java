package com.erp.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入通用结果。
 */
@Data
public class BatchImportResult {

    private int successCount;
    private List<FailItem> failList = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailItem {
        private int index;
        private String name;
        private String reason;
    }
}
