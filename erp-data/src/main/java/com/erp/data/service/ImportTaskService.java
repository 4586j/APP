package com.erp.data.service;

import com.erp.data.dto.ImportTaskVO;

public interface ImportTaskService {

    /**
     * 创建并初始化一个导入任务。
     */
    ImportTaskVO createTask(String taskId, String fileName);

    /**
     * 更新任务为运行中。
     */
    void markRunning(String taskId);

    /**
     * 更新解析/导入进度。
     */
    void updateProgress(String taskId, long totalRows, long processedRows,
                        long successCount, long failCount);

    /**
     * 追加失败明细(上限内才保存快照)。
     */
    void addFailItem(String taskId, ImportTaskVO.FailItem item);

    /**
     * 标记任务完成。
     */
    void markDone(String taskId, String message);

    /**
     * 标记任务失败。
     */
    void markFailed(String taskId, String message);

    /**
     * 查询任务当前进度/结果。
     */
    ImportTaskVO getTask(String taskId);
}
