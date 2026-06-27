package com.erp.data.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.data.dto.ImportTaskVO;
import com.erp.data.dto.PricingImportExcelDTO;
import com.erp.data.entity.DatPricingAnalysis;
import com.erp.data.service.ImportTaskService;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 定价分析 Excel 流式读取监听器。
 *
 * <p>特点：
 * <ul>
 *   <li>按 batchSize 累积，调用 MyBatis-Plus saveBatch 批量入库；</li>
 *   <li>每读一行实时更新 Redis 进度；</li>
 *   <li>失败行记录到 Redis 失败快照（最多保留 1000 条）；</li>
 *   <li>整批失败不影响其他批次（每批独立事务）。</li>
 * </ul>
 */
@Slf4j
public class PricingImportListener implements ReadListener<PricingImportExcelDTO> {

    private static final int BATCH_SIZE = 500;

    private final IService<DatPricingAnalysis> service;
    private final ImportTaskService taskService;
    private final String taskId;

    /** 当前内存中累积的一批。 */
    private List<DatPricingAnalysis> cachedDataList;

    private long totalRows = 0;
    private long processedRows = 0;
    private long successCount = 0;
    private long failCount = 0;

    public PricingImportListener(IService<DatPricingAnalysis> service,
                                 ImportTaskService taskService,
                                 String taskId) {
        this.service = service;
        this.taskService = taskService;
        this.taskId = taskId;
        this.cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_SIZE);
    }

    @Override
    public void invoke(PricingImportExcelDTO dto, AnalysisContext context) {
        totalRows++;
        processedRows++;

        int rowIndex = context.readRowHolder().getRowIndex() + 1; // Excel 行号从 1 开始
        if (dto == null) {
            addFail(rowIndex, "", "空行");
            flushProgress();
            return;
        }

        String title = dto.getTitle();
        if (dto.getProductId() == null || title == null || title.isBlank()) {
            addFail(rowIndex, title, "产品ID和分析标题不能为空");
            flushProgress();
            return;
        }

        DatPricingAnalysis e = new DatPricingAnalysis();
        e.setProductId(dto.getProductId());
        e.setTitle(title);
        e.setCostPrice(toBigDecimal(dto.getCostPrice()));
        e.setTargetPrice(toBigDecimal(dto.getTargetPrice()));
        e.setCompetitorPrice(toBigDecimal(dto.getCompetitorPrice()));
        e.setSuggestedPrice(toBigDecimal(dto.getSuggestedPrice()));
        e.setMargin(toBigDecimal(dto.getMargin()));
        e.setMarketTrend(trim(dto.getMarketTrend()));
        e.setStatus(trim(dto.getStatus()) != null ? trim(dto.getStatus()) : "draft");
        e.setRemark(trim(dto.getRemark()));

        cachedDataList.add(e);
        if (cachedDataList.size() >= BATCH_SIZE) {
            saveBatch(rowIndex);
        } else {
            // 小批量累积过程中也刷新进度,让前端感知到实时读取
            flushProgress();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 处理最后不足一批的数据
        if (!cachedDataList.isEmpty()) {
            saveBatch(0);
        }
        log.info("导入任务完成, taskId={}, totalRows={}, successCount={}, failCount={}",
            taskId, totalRows, successCount, failCount);
    }

    /**
     * 每批独立事务: 失败时只回滚本批次, successes 不回滚。
     */
    private void saveBatch(int lastRowIndex) {
        List<DatPricingAnalysis> batch = new ArrayList<>(cachedDataList);
        cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_SIZE);
        try {
            service.saveBatch(batch);
            successCount += batch.size();
        } catch (Exception e) {
            log.warn("批量保存失败, taskId={}, batchSize={}, 回退逐条保存, err={}",
                taskId, batch.size(), e.getMessage());
            // 逐条保存,单条失败记录原因,成功计入 successCount
            for (DatPricingAnalysis item : batch) {
                try {
                    service.save(item);
                    successCount++;
                } catch (Exception ex) {
                    failCount++;
                    taskService.addFailItem(taskId,
                        new ImportTaskVO.FailItem(lastRowIndex, item.getTitle(),
                            "保存失败: " + ex.getMessage()));
                }
            }
        }
        flushProgress();
    }

    private void addFail(int rowIndex, String name, String reason) {
        failCount++;
        taskService.addFailItem(taskId, new ImportTaskVO.FailItem(rowIndex, name, reason));
    }

    private void flushProgress() {
        taskService.updateProgress(taskId, totalRows, processedRows, successCount, failCount);
    }

    private static BigDecimal toBigDecimal(BigDecimal v) {
        return v != null ? v : null;
    }

    private static String trim(String s) {
        return s != null ? s.trim() : null;
    }
}
