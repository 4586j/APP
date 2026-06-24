package com.erp.customer.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.customer.dto.*;
import com.erp.customer.entity.CustCustomer;
import com.erp.customer.mapper.CustCustomerMapper;
import com.erp.customer.service.CustCustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class CustCustomerServiceImpl implements CustCustomerService {
    final CustCustomerMapper customerMapper;

    CustomerVO toVO(CustCustomer x) {
        return CustomerVO.builder().id(x.getId()).customerCode(x.getCustomerCode())
            .customerName(x.getCustomerName()).customerNameEn(x.getCustomerNameEn())
            .customerType(x.getCustomerType()).country(x.getCountry())
            .contactPerson(x.getContactPerson()).contactEmail(x.getContactEmail())
            .contactPhone(x.getContactPhone()).contactFax(x.getContactFax())
            .website(x.getWebsite()).address(x.getAddress())
            .creditLimit(x.getCreditLimit()).paymentTerms(x.getPaymentTerms())
            .taxId(x.getTaxId()).swiftCode(x.getSwiftCode())
            .bankName(x.getBankName()).bankAccount(x.getBankAccount())
            .status(x.getStatus()).createdAt(x.getCreatedAt()).build();
    }

    public CustomerPageVO listPage(CustomerQuery q) {
        if (q.getPage()==null||q.getPage()<1) q.setPage(1);
        if (q.getSize()==null||q.getSize()<1) q.setSize(10);
        var w = new LambdaQueryWrapper<CustCustomer>();
        if (q.getKeyword()!=null&&!q.getKeyword().isEmpty())
            w.and(x->x.like(CustCustomer::getCustomerName,q.getKeyword()).or().like(CustCustomer::getCustomerCode,q.getKeyword()));
        if (q.getCountry()!=null&&!q.getCountry().isEmpty()) w.eq(CustCustomer::getCountry,q.getCountry());
        if (q.getCustomerType()!=null&&!q.getCustomerType().isEmpty()) w.eq(CustCustomer::getCustomerType,q.getCustomerType());
        w.orderByDesc(CustCustomer::getCreatedAt);
        var p = customerMapper.selectPage(new Page<>(q.getPage(),Math.min(q.getSize(),100)), w);
        return CustomerPageVO.builder().records(p.getRecords().stream().map(this::toVO).toList())
            .total(p.getTotal()).size(p.getSize()).current(p.getCurrent()).build();
    }

    public CustomerVO getById(Long id) {
        var x = customerMapper.selectById(id);
        if (x == null) throw new BusinessException(R.CODE_NOT_FOUND, "customer not found");
        return toVO(x);
    }

    @Transactional public Long create(CustomerCreateRequest r) {
        var c = new CustCustomer();
        c.setCustomerCode(r.getCustomerCode()); c.setCustomerName(r.getCustomerName());
        c.setCustomerNameEn(r.getCustomerNameEn()); c.setCustomerType(r.getCustomerType());
        c.setCountry(r.getCountry()); c.setContactPerson(r.getContactPerson());
        c.setContactEmail(r.getContactEmail()); c.setContactPhone(r.getContactPhone());
        c.setContactFax(r.getContactFax()); c.setWebsite(r.getWebsite()); c.setAddress(r.getAddress());
        c.setCreditLimit(r.getCreditLimit()); c.setPaymentTerms(r.getPaymentTerms());
        c.setTaxId(r.getTaxId()); c.setSwiftCode(r.getSwiftCode());
        c.setBankName(r.getBankName()); c.setBankAccount(r.getBankAccount());
        c.setStatus(1); customerMapper.insert(c); return c.getId();
    }

    @Transactional public void update(Long id, CustomerCreateRequest r) {
        var c = customerMapper.selectById(id);
        if (c == null) throw new BusinessException(R.CODE_NOT_FOUND, "customer not found");
        if (r.getCustomerName()!=null) c.setCustomerName(r.getCustomerName());
        if (r.getCustomerType()!=null) c.setCustomerType(r.getCustomerType());
        if (r.getCountry()!=null) c.setCountry(r.getCountry());
        if (r.getContactPerson()!=null) c.setContactPerson(r.getContactPerson());
        if (r.getContactEmail()!=null) c.setContactEmail(r.getContactEmail());
        if (r.getContactPhone()!=null) c.setContactPhone(r.getContactPhone());
        if (r.getCreditLimit()!=null) c.setCreditLimit(r.getCreditLimit());
        if (r.getPaymentTerms()!=null) c.setPaymentTerms(r.getPaymentTerms());
        if (r.getTaxId()!=null) c.setTaxId(r.getTaxId());
        if (r.getSwiftCode()!=null) c.setSwiftCode(r.getSwiftCode());
        if (r.getBankName()!=null) c.setBankName(r.getBankName());
        if (r.getBankAccount()!=null) c.setBankAccount(r.getBankAccount());
        customerMapper.updateById(c);
    }

    @Transactional public void delete(Long id) { customerMapper.deleteById(id); }
}