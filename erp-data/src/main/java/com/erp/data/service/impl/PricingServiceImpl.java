package com.erp.data.service.impl;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.dto.BatchImportResult;
import com.erp.data.dto.PricingImportExcelDTO;
import com.erp.data.dto.PricingPageVO; import com.erp.data.dto.PricingQuery;
import com.erp.data.dto.PricingVO; import com.erp.data.dto.PricingCreateRequest;
import com.erp.data.entity.DatPricingAnalysis;
import com.erp.data.mapper.DatPricingAnalysisMapper;
import com.erp.data.service.PricingService;
import lombok.RequiredArgsConstructor; import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
@Service @RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {
    final DatPricingAnalysisMapper mapper;
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
    public Long create(PricingCreateRequest r) {
        DatPricingAnalysis e = new DatPricingAnalysis();
        BeanUtils.copyProperties(r, e);
        if (e.getStatus() == null) e.setStatus("draft");
        mapper.insert(e); return e.getId();
    }
    public void update(Long id, PricingCreateRequest r) {
        DatPricingAnalysis e = mapper.selectById(id);
        if (e != null) { BeanUtils.copyProperties(r, e); mapper.updateById(e); }
    }
    public void delete(Long id) { mapper.deleteById((Long)id); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchImportResult batchImport(InputStream inputStream) {
        BatchImportResult result = new BatchImportResult();
        List<PricingImportExcelDTO> list = EasyExcel.read(inputStream)
            .head(PricingImportExcelDTO.class)
            .sheet()
            .doReadSync();
        if (list == null || list.isEmpty()) return result;

        List<DatPricingAnalysis> toInsert = new java.util.ArrayList<>();
        int index = 1;
        for (PricingImportExcelDTO dto : list) {
            index++;
            if (dto.getProductId() == null || dto.getTitle() == null || dto.getTitle().isEmpty()) {
                result.getFailList().add(new BatchImportResult.FailItem(index, dto.getTitle(), "产品ID和分析标题不能为空"));
                continue;
            }
            DatPricingAnalysis e = new DatPricingAnalysis();
            e.setProductId(dto.getProductId());
            e.setTitle(dto.getTitle());
            e.setCostPrice(dto.getCostPrice());
            e.setTargetPrice(dto.getTargetPrice());
            e.setCompetitorPrice(dto.getCompetitorPrice());
            e.setSuggestedPrice(dto.getSuggestedPrice());
            e.setMargin(dto.getMargin());
            e.setMarketTrend(dto.getMarketTrend());
            e.setStatus(dto.getStatus() != null ? dto.getStatus() : "draft");
            e.setRemark(dto.getRemark());
            toInsert.add(e);
        }
        // 批量插入，减少数据库往返
        if (!toInsert.isEmpty()) {
            for (DatPricingAnalysis e : toInsert) {
                mapper.insert(e);
            }
            result.setSuccessCount(toInsert.size());
        }
        return result;
    }

    private PricingVO toVO(DatPricingAnalysis e) {
        if (e == null) return null;
        PricingVO v = new PricingVO(); BeanUtils.copyProperties(e, v); return v;
    }
}