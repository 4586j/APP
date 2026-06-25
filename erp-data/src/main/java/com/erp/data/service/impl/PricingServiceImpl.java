package com.erp.data.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.data.dto.PricingPageVO; import com.erp.data.dto.PricingQuery;
import com.erp.data.dto.PricingVO; import com.erp.data.dto.PricingCreateRequest;
import com.erp.data.entity.DatPricingAnalysis;
import com.erp.data.mapper.DatPricingAnalysisMapper;
import com.erp.data.service.PricingService;
import lombok.RequiredArgsConstructor; import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.io.Serializable;
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
        Page<DatPricingAnalysis> p = mapper.selectPage(new Page<>(q.getPageNum(), q.getPageSize()), w);
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
    private PricingVO toVO(DatPricingAnalysis e) {
        if (e == null) return null;
        PricingVO v = new PricingVO(); BeanUtils.copyProperties(e, v); return v;
    }
}