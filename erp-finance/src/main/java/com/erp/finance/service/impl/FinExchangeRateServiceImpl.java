package com.erp.finance.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.finance.dto.*;
import com.erp.finance.entity.FinExchangeRate;
import com.erp.finance.mapper.FinExchangeRateMapper;
import com.erp.finance.service.FinExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class FinExchangeRateServiceImpl implements FinExchangeRateService {
    final FinExchangeRateMapper rateMapper;
    public ExchangeRatePageVO listPage(ExchangeRateQuery q) {
        if (q.getPage()==null||q.getPage()<1)q.setPage(1);
        if (q.getSize()==null||q.getSize()<1)q.setSize(20);
        var w=new LambdaQueryWrapper<FinExchangeRate>();
        if (q.getDate()!=null) w.eq(FinExchangeRate::getRateDate,q.getDate());
        if (q.getCurrency()!=null) w.eq(FinExchangeRate::getCurrencyFrom,q.getCurrency());
        w.orderByDesc(FinExchangeRate::getRateDate).orderByDesc(FinExchangeRate::getCreatedAt);
        var p=rateMapper.selectPage(new Page<>(q.getPage(),Math.min(q.getSize(),100)),w);
        return ExchangeRatePageVO.builder()
            .records(p.getRecords().stream().map(r->ExchangeRateVO.builder().id(r.getId()).currencyFrom(r.getCurrencyFrom()).currencyTo(r.getCurrencyTo()).rate(r.getRate()).rateDate(r.getRateDate()).source(r.getSource()).createdAt(r.getCreatedAt()).build()).toList())
            .total(p.getTotal()).size(p.getSize()).current(p.getCurrent()).build();
    }
    @Transactional public Long create(ExchangeRateCreateRequest r){
        var e=new FinExchangeRate(); e.setCurrencyFrom(r.getCurrencyFrom());
        e.setCurrencyTo(r.getCurrencyTo()); e.setRate(r.getRate());
        e.setRateDate(r.getRateDate()); e.setSource(r.getSource());
        rateMapper.insert(e); return e.getId();
    }
    @Transactional public void delete(Long id){rateMapper.deleteById(id);}
}