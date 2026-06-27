package com.erp.data.service.impl;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.data.dto.ImportTaskVO;
import com.erp.data.dto.PricingCreateRequest;
import com.erp.data.dto.PricingImportExcelDTO;
import com.erp.data.dto.PricingPageVO; import com.erp.data.dto.PricingQuery;
import com.erp.data.dto.PricingVO;
import com.erp.data.entity.DatPricingAnalysis;
import com.erp.data.listener.PricingImportListener;
import com.erp.data.mapper.DatPricingAnalysisMapper;
import com.erp.data.service.ImportTaskService;
import com.erp.data.service.PricingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PricingServiceImpl extends ServiceImpl<DatPricingAnalysisMapper, DatPricingAnalysis> implements PricingService {

    private final DatPricingAnalysisMapper mapper;
    private final ImportTaskService importTaskService;

    public PricingServiceImpl(DatPricingAnalysisMapper mapper,
                              ImportTaskService importTaskService) {
        this.mapper = mapper;
        this.importTaskService = importTaskService;
    }

    public PricingPageVO listPage(PricingQuery q) {
        LambdaQueryWrapper<DatPricingAnalysis> w = new LambdaQueryWrapper<>();
        if (q.getProductId() != null) w.eq(DatPricingAnalysis::getProductId, q.getProductId());
        if (q.getStatus() != null && !q.getStatus().isEmpty())
            w.eq(DatPricingAnalysis::getStatus, q.getStatus());
        w.orderByDesc(DatPricingAnalysis::getCreatedAt);
        Page<DatPricingAnalysis> p = mapper.selectPage(new Page<>(q.getPageNum(), Math.min(q.getPageSize(), 100)), w);
        return new PricingPageVO(p.getTotal(), q.getPageNum(), q.getPageSize(),
            p.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    public PricingVO getById(Long id) { return toVO(mapper.selectById(id)); }

    @Transactional(rollbackFor = Exception.class)
    public Long create(PricingCreateRequest r) {
        DatPricingAnalysis e = new DatPricingAnalysis();
        BeanUtils.copyProperties(r, e);
        if (e.getStatus() == null) e.setStatus("draft");
        mapper.insert(e); return e.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PricingCreateRequest r) {
        DatPricingAnalysis e = mapper.selectById(id);
        if (e != null) { BeanUtils.copyProperties(r, e); mapper.updateById(e); }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) { mapper.deleteById((Long)id); }

    @Override
    public ImportTaskVO submitImportTask(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的文件");
        }
        String taskId = UUID.randomUUID().toString().replace("-", "");
        String originalName = file.getOriginalFilename() == null ? "unknown.xlsx" : file.getOriginalFilename();
        importTaskService.createTask(taskId, originalName);

        Path tempFile;
        try {
            tempFile = Files.createTempFile("pricing-import-" + taskId + "-", ".xlsx");
            file.transferTo(tempFile);
        } catch (IOException e) {
            importTaskService.markFailed(taskId, "文件转存失败: " + e.getMessage());
            throw new IllegalStateException("文件转存失败", e);
        }

        // 异步执行解析+导入
        doImportAsync(taskId, tempFile);
        return importTaskService.getTask(taskId);
    }

    @Async("importTaskExecutor")
    public void doImportAsync(String taskId, Path tempFile) {
        importTaskService.markRunning(taskId);
        try (InputStream in = Files.newInputStream(tempFile)) {
            PricingImportListener listener = new PricingImportListener(this, importTaskService, taskId);
            EasyExcel.read(in, PricingImportExcelDTO.class, listener).sheet().doRead();
            ImportTaskVO finalTask = importTaskService.getTask(taskId);
            String message = String.format("导入完成: 成功 %d 条, 失败 %d 条",
                finalTask.getSuccessCount() != null ? finalTask.getSuccessCount() : 0,
                finalTask.getFailCount() != null ? finalTask.getFailCount() : 0);
            importTaskService.markDone(taskId, message);
        } catch (Exception e) {
            log.error("导入任务执行失败, taskId={}", taskId, e);
            importTaskService.markFailed(taskId, "导入失败: " + e.getMessage());
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                log.warn("临时文件删除失败, path={}: {}", tempFile, e.getMessage());
            }
        }
    }

    @Override
    public ImportTaskVO getImportTask(String taskId) {
        return importTaskService.getTask(taskId);
    }

    private PricingVO toVO(DatPricingAnalysis e) {
        if (e == null) return null;
        PricingVO v = new PricingVO(); BeanUtils.copyProperties(e, v); return v;
    }
}
