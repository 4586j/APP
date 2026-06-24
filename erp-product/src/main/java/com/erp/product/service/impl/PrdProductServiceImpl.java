package com.erp.product.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.product.dto.*;
import com.erp.product.entity.PrdProduct;
import com.erp.product.mapper.PrdProductMapper;
import com.erp.product.service.PrdProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class PrdProductServiceImpl implements PrdProductService {
    final PrdProductMapper productMapper;
    public ProductPageVO listPage(ProductQuery q) {
        if (q.getPage()==null||q.getPage()<1) q.setPage(1);
        if (q.getSize()==null||q.getSize()<1) q.setSize(10);
        var w = new LambdaQueryWrapper<PrdProduct>();
        if (q.getKeyword()!=null && !q.getKeyword().isEmpty()) w.and(x->x.like(PrdProduct::getProductName,q.getKeyword()).or().like(PrdProduct::getProductCode,q.getKeyword()));
        if (q.getCategoryId()!=null) w.eq(PrdProduct::getCategoryId,q.getCategoryId());
        if (q.getHsCode()!=null && !q.getHsCode().isEmpty()) w.like(PrdProduct::getHsCode,q.getHsCode());
        w.orderByDesc(PrdProduct::getCreatedAt);
        var p = productMapper.selectPage(new Page<>(q.getPage(),Math.min(q.getSize(),100)), w);
        return ProductPageVO.builder().records(p.getRecords().stream().map(this::toVO).toList()).total(p.getTotal()).size(p.getSize()).current(p.getCurrent()).build();
    }
    ProductVO toVO(PrdProduct x){ return ProductVO.builder().id(x.getId()).productCode(x.getProductCode()).productName(x.getProductName()).productNameEn(x.getProductNameEn()).categoryId(x.getCategoryId()).hsCodeId(x.getHsCodeId()).hsCode(x.getHsCode()).unit(x.getUnit()).specification(x.getSpecification()).originCountry(x.getOriginCountry()).brand(x.getBrand()).purchasePrice(x.getPurchasePrice()).salesPrice(x.getSalesPrice()).costPrice(x.getCostPrice()).weightKg(x.getWeightKg()).volumeCbm(x.getVolumeCbm()).moq(x.getMoq()).status(x.getStatus()).createdAt(x.getCreatedAt()).build(); }
    public ProductVO getById(Long id){ var x=productMapper.selectById(id); if(x==null)throw new BusinessException(R.CODE_NOT_FOUND,"product not found"); return toVO(x); }
    @Transactional public Long create(ProductCreateRequest r){ var p=new PrdProduct(); p.setProductCode(r.getProductCode());p.setProductName(r.getProductName());p.setProductNameEn(r.getProductNameEn());p.setCategoryId(r.getCategoryId());p.setHsCodeId(r.getHsCodeId());p.setUnit(r.getUnit()!=null?r.getUnit():"件");p.setSpecification(r.getSpecification());p.setOriginCountry(r.getOriginCountry());p.setBrand(r.getBrand());p.setPurchasePrice(r.getPurchasePrice());p.setSalesPrice(r.getSalesPrice());p.setCostPrice(r.getCostPrice());p.setWeightKg(r.getWeightKg());p.setVolumeCbm(r.getVolumeCbm());p.setMoq(r.getMoq());p.setStatus(1);productMapper.insert(p);return p.getId(); }
    @Transactional public void update(Long id, ProductCreateRequest r){ var p=productMapper.selectById(id);if(p==null)throw new BusinessException(R.CODE_NOT_FOUND,"product not found");if(r.getProductName()!=null)p.setProductName(r.getProductName());if(r.getCategoryId()!=null)p.setCategoryId(r.getCategoryId());if(r.getUnit()!=null)p.setUnit(r.getUnit());if(r.getSalesPrice()!=null)p.setSalesPrice(r.getSalesPrice());if(r.getCostPrice()!=null)p.setCostPrice(r.getCostPrice());productMapper.updateById(p); }
    @Transactional public void delete(Long id){ productMapper.deleteById(id); }
}