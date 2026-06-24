package com.erp.customer.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.customer.dto.*;
import com.erp.customer.entity.CustSupplier;
import com.erp.customer.mapper.CustSupplierMapper;
import com.erp.customer.service.CustSupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class CustSupplierServiceImpl implements CustSupplierService {
    final CustSupplierMapper supplierMapper;

    SupplierVO toVO(CustSupplier x) {
        return SupplierVO.builder().id(x.getId()).supplierCode(x.getSupplierCode())
            .supplierName(x.getSupplierName()).province(x.getProvince()).city(x.getCity())
            .contactPerson(x.getContactPerson()).contactPhone(x.getContactPhone())
            .contactEmail(x.getContactEmail()).address(x.getAddress()).rating(x.getRating())
            .paymentTerms(x.getPaymentTerms()).bankName(x.getBankName()).bankAccount(x.getBankAccount())
            .taxId(x.getTaxId()).mainProducts(x.getMainProducts())
            .cooperationYears(x.getCooperationYears()).status(x.getStatus())
            .createdAt(x.getCreatedAt()).build();
    }

    public SupplierPageVO listPage(SupplierQuery q) {
        if (q.getPage()==null||q.getPage()<1) q.setPage(1);
        if (q.getSize()==null||q.getSize()<1) q.setSize(10);
        var w = new LambdaQueryWrapper<CustSupplier>();
        if (q.getKeyword()!=null&&!q.getKeyword().isEmpty())
            w.and(x->x.like(CustSupplier::getSupplierName,q.getKeyword()).or().like(CustSupplier::getSupplierCode,q.getKeyword()));
        if (q.getProvince()!=null&&!q.getProvince().isEmpty()) w.eq(CustSupplier::getProvince,q.getProvince());
        if (q.getRating()!=null) w.eq(CustSupplier::getRating,q.getRating());
        w.orderByDesc(CustSupplier::getCreatedAt);
        var p = supplierMapper.selectPage(new Page<>(q.getPage(),Math.min(q.getSize(),100)), w);
        return SupplierPageVO.builder().records(p.getRecords().stream().map(this::toVO).toList())
            .total(p.getTotal()).size(p.getSize()).current(p.getCurrent()).build();
    }

    public SupplierVO getById(Long id) {
        var x = supplierMapper.selectById(id);
        if (x == null) throw new BusinessException(R.CODE_NOT_FOUND, "supplier not found");
        return toVO(x);
    }

    @Transactional public Long create(SupplierCreateRequest r) {
        var s = new CustSupplier();
        s.setSupplierCode(r.getSupplierCode()); s.setSupplierName(r.getSupplierName());
        s.setProvince(r.getProvince()); s.setCity(r.getCity());
        s.setContactPerson(r.getContactPerson()); s.setContactPhone(r.getContactPhone());
        s.setContactEmail(r.getContactEmail()); s.setAddress(r.getAddress());
        s.setRating(r.getRating()!=null?r.getRating():0);
        s.setPaymentTerms(r.getPaymentTerms()); s.setBankName(r.getBankName());
        s.setBankAccount(r.getBankAccount()); s.setTaxId(r.getTaxId());
        s.setMainProducts(r.getMainProducts()); s.setCooperationYears(r.getCooperationYears());
        s.setStatus(1); supplierMapper.insert(s); return s.getId();
    }

    @Transactional public void update(Long id, SupplierCreateRequest r) {
        var s = supplierMapper.selectById(id);
        if (s == null) throw new BusinessException(R.CODE_NOT_FOUND, "supplier not found");
        if (r.getSupplierName()!=null) s.setSupplierName(r.getSupplierName());
        if (r.getProvince()!=null) s.setProvince(r.getProvince());
        if (r.getCity()!=null) s.setCity(r.getCity());
        if (r.getContactPerson()!=null) s.setContactPerson(r.getContactPerson());
        if (r.getContactPhone()!=null) s.setContactPhone(r.getContactPhone());
        if (r.getRating()!=null) s.setRating(r.getRating());
        if (r.getPaymentTerms()!=null) s.setPaymentTerms(r.getPaymentTerms());
        if (r.getBankName()!=null) s.setBankName(r.getBankName());
        if (r.getBankAccount()!=null) s.setBankAccount(r.getBankAccount());
        if (r.getMainProducts()!=null) s.setMainProducts(r.getMainProducts());
        supplierMapper.updateById(s);
    }

    @Transactional public void delete(Long id) { supplierMapper.deleteById(id); }
}