package com.erp.finance.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.finance.dto.*;
import com.erp.finance.entity.FinPayable;
import com.erp.finance.mapper.FinPayableMapper;
import com.erp.finance.service.FinPayableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate; import java.time.format.DateTimeFormatter; import java.time.temporal.ChronoUnit;
import java.util.ArrayList; import java.util.List;

@Service @RequiredArgsConstructor
public class FinPayableServiceImpl implements FinPayableService {
    final FinPayableMapper mapper;
    @Override public PayablePageVO listPage(PayableQuery q){
        if(q.getPage()==null||q.getPage()<1)q.setPage(1); if(q.getSize()==null||q.getSize()<1)q.setSize(20);
        var w=new LambdaQueryWrapper<FinPayable>().eq(FinPayable::getDeleted,0);
        if(q.getStatus()!=null)w.eq(FinPayable::getStatus,q.getStatus());
        if(q.getSupplierId()!=null)w.eq(FinPayable::getSupplierId,q.getSupplierId());
        if(q.getKeyword()!=null)w.like(FinPayable::getPayNo,"%"+q.getKeyword()+"%").or().like(FinPayable::getSupplierName,"%"+q.getKeyword()+"%");
        w.orderByDesc(FinPayable::getCreatedAt);
        var p=mapper.selectPage(new Page<>(q.getPage(),Math.min(q.getSize(),100)),w);
        var records=p.getRecords().stream().map(e->PayableVO.builder().id(e.getId()).payNo(e.getPayNo())
            .sourceType(e.getSourceType()).sourceId(e.getSourceId()).supplierId(e.getSupplierId()).supplierName(e.getSupplierName())
            .totalAmount(e.getTotalAmount()).paidAmount(e.getPaidAmount())
            .balance(e.getTotalAmount().subtract(e.getPaidAmount()!=null?e.getPaidAmount():java.math.BigDecimal.ZERO))
            .dueDate(e.getDueDate()).currency(e.getCurrency()).status(e.getStatus()).remark(e.getRemark()).createdAt(e.getCreatedAt()).build()).toList();
        return PayablePageVO.builder().records(records).total(p.getTotal()).size(p.getSize()).current(p.getCurrent()).build();
    }
    @Override public PayableVO getById(Long id){
        var e=mapper.selectById(id); if(e==null)return null;
        return PayableVO.builder().id(e.getId()).payNo(e.getPayNo()).sourceType(e.getSourceType()).sourceId(e.getSourceId())
            .supplierId(e.getSupplierId()).supplierName(e.getSupplierName()).totalAmount(e.getTotalAmount()).paidAmount(e.getPaidAmount())
            .balance(e.getTotalAmount().subtract(e.getPaidAmount()!=null?e.getPaidAmount():java.math.BigDecimal.ZERO))
            .dueDate(e.getDueDate()).currency(e.getCurrency()).status(e.getStatus()).remark(e.getRemark()).createdAt(e.getCreatedAt()).build();
    }
    @Override @Transactional public Long create(PayableCreateRequest r){
        var datePart=LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var e=new FinPayable(); e.setPayNo("PAY-"+datePart+"-"+System.currentTimeMillis()%100000);
        e.setSourceType(r.getSourceType()); e.setSourceId(r.getSourceId());
        e.setSupplierId(r.getSupplierId()); e.setSupplierName(r.getSupplierName());
        e.setTotalAmount(r.getTotalAmount()); e.setDueDate(r.getDueDate()); e.setRemark(r.getRemark());
        mapper.insert(e); return e.getId();
    }
    @Override @Transactional public void update(Long id, PayableUpdateRequest r){
        var e=mapper.selectById(id);
        if(e==null) throw new BusinessException(R.CODE_NOT_FOUND,"应付单不存在");
        if(r.getSupplierName()!=null) e.setSupplierName(r.getSupplierName());
        if(r.getTotalAmount()!=null) e.setTotalAmount(r.getTotalAmount());
        if(r.getDueDate()!=null) e.setDueDate(r.getDueDate());
        if(r.getRemark()!=null) e.setRemark(r.getRemark());
        e.setStatus(deriveStatus(e.getTotalAmount(), e.getPaidAmount()));
        mapper.updateById(e);
    }
    @Override @Transactional public void delete(Long id){mapper.deleteById(id);}

    @Override @Transactional public void confirmPayment(Long id, ConfirmPaymentRequest r){
        var e=mapper.selectById(id);
        if(e==null) throw new BusinessException(R.CODE_NOT_FOUND,"应付单不存在");
        var amt=r.getAmount()!=null?r.getAmount():BigDecimal.ZERO;
        if(amt.signum()<=0) throw new BusinessException(R.CODE_PARAM_INVALID,"确认金额必须大于0");
        var paid=(e.getPaidAmount()!=null?e.getPaidAmount():BigDecimal.ZERO).add(amt);
        if(paid.compareTo(e.getTotalAmount())>0) throw new BusinessException(R.CODE_PARAM_INVALID,"已付金额超过应付总额");
        e.setPaidAmount(paid);
        e.setStatus(deriveStatus(e.getTotalAmount(), paid));
        if(r.getRemark()!=null) e.setRemark(r.getRemark());
        mapper.updateById(e);
    }

    @Override @Transactional public int batchConfirm(List<Long> ids){
        if(ids==null||ids.isEmpty()) return 0;
        int n=0;
        for(Long id:ids){
            var e=mapper.selectById(id);
            if(e==null) continue;
            e.setPaidAmount(e.getTotalAmount());
            e.setStatus("paid");
            mapper.updateById(e); n++;
        }
        return n;
    }

    @Override public AgingReportVO aging(){
        var list=mapper.selectList(new LambdaQueryWrapper<FinPayable>()
            .eq(FinPayable::getDeleted,0).ne(FinPayable::getStatus,"paid"));
        int[][] ranges={{0,0},{1,30},{31,60},{61,90},{91,-1}};
        String[] labels={"未到期","1-30天","31-60天","61-90天","90天以上"};
        var buckets=new ArrayList<AgingReportVO.Bucket>();
        var amounts=new BigDecimal[ranges.length]; var counts=new long[ranges.length];
        for(int i=0;i<ranges.length;i++) amounts[i]=BigDecimal.ZERO;
        var total=BigDecimal.ZERO; var today=LocalDate.now();
        for(var e:list){
            var bal=e.getTotalAmount().subtract(e.getPaidAmount()!=null?e.getPaidAmount():BigDecimal.ZERO);
            if(bal.signum()<=0) continue;
            total=total.add(bal);
            long overdue=e.getDueDate()==null?0:ChronoUnit.DAYS.between(e.getDueDate(),today);
            int idx;
            if(overdue<=0) idx=0;
            else if(overdue<=30) idx=1;
            else if(overdue<=60) idx=2;
            else if(overdue<=90) idx=3;
            else idx=4;
            amounts[idx]=amounts[idx].add(bal); counts[idx]++;
        }
        for(int i=0;i<ranges.length;i++)
            buckets.add(AgingReportVO.Bucket.builder().label(labels[i]).minDays(ranges[i][0]).maxDays(ranges[i][1])
                .count(counts[i]).amount(amounts[i]).build());
        return AgingReportVO.builder().buckets(buckets).totalOutstanding(total).build();
    }

    /** 状态推导：已付>=总额→paid；0<已付<总额→partial；否则 pending */
    static String deriveStatus(BigDecimal total, BigDecimal paid){
        var p=paid!=null?paid:BigDecimal.ZERO;
        if(p.compareTo(total)>=0) return "paid";
        if(p.signum()>0) return "partial";
        return "pending";
    }
}