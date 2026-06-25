package com.erp.finance.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.finance.dto.*;
import com.erp.finance.entity.FinReceivable;
import com.erp.finance.mapper.FinReceivableMapper;
import com.erp.finance.service.FinReceivableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate; import java.time.format.DateTimeFormatter; import java.time.temporal.ChronoUnit;
import java.util.ArrayList; import java.util.List;

@Service @RequiredArgsConstructor
public class FinReceivableServiceImpl implements FinReceivableService {
    final FinReceivableMapper mapper;
    @Override public ReceivablePageVO listPage(ReceivableQuery q){
        if(q.getPage()==null||q.getPage()<1)q.setPage(1); if(q.getSize()==null||q.getSize()<1)q.setSize(20);
        var w=new LambdaQueryWrapper<FinReceivable>().eq(FinReceivable::getDeleted,0);
        if(q.getStatus()!=null)w.eq(FinReceivable::getStatus,q.getStatus());
        if(q.getCustomerId()!=null)w.eq(FinReceivable::getCustomerId,q.getCustomerId());
        if(q.getKeyword()!=null)w.like(FinReceivable::getReceiptNo,"%"+q.getKeyword()+"%").or().like(FinReceivable::getCustomerName,"%"+q.getKeyword()+"%");
        w.orderByDesc(FinReceivable::getCreatedAt);
        var p=mapper.selectPage(new Page<>(q.getPage(),Math.min(q.getSize(),100)),w);
        var records=p.getRecords().stream().map(e->ReceivableVO.builder().id(e.getId()).receiptNo(e.getReceiptNo())
            .sourceType(e.getSourceType()).sourceId(e.getSourceId()).customerId(e.getCustomerId()).customerName(e.getCustomerName())
            .totalAmount(e.getTotalAmount()).receivedAmount(e.getReceivedAmount())
            .balance(e.getTotalAmount().subtract(e.getReceivedAmount()!=null?e.getReceivedAmount():java.math.BigDecimal.ZERO))
            .dueDate(e.getDueDate()).currency(e.getCurrency()).status(e.getStatus()).remark(e.getRemark()).createdAt(e.getCreatedAt()).build()).toList();
        return ReceivablePageVO.builder().records(records).total(p.getTotal()).size(p.getSize()).current(p.getCurrent()).build();
    }
    @Override public ReceivableVO getById(Long id){
        var e=mapper.selectById(id); if(e==null)return null;
        return ReceivableVO.builder().id(e.getId()).receiptNo(e.getReceiptNo()).sourceType(e.getSourceType()).sourceId(e.getSourceId())
            .customerId(e.getCustomerId()).customerName(e.getCustomerName()).totalAmount(e.getTotalAmount()).receivedAmount(e.getReceivedAmount())
            .balance(e.getTotalAmount().subtract(e.getReceivedAmount()!=null?e.getReceivedAmount():java.math.BigDecimal.ZERO))
            .dueDate(e.getDueDate()).currency(e.getCurrency()).status(e.getStatus()).remark(e.getRemark()).createdAt(e.getCreatedAt()).build();
    }
    @Override @Transactional public Long create(ReceivableCreateRequest r){
        var datePart=LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var e=new FinReceivable(); e.setReceiptNo("REC-"+datePart+"-"+System.currentTimeMillis()%100000);
        e.setSourceType(r.getSourceType()); e.setSourceId(r.getSourceId());
        e.setCustomerId(r.getCustomerId()); e.setCustomerName(r.getCustomerName());
        e.setTotalAmount(r.getTotalAmount()); e.setDueDate(r.getDueDate()); e.setRemark(r.getRemark());
        mapper.insert(e); return e.getId();
    }
    @Override @Transactional public void update(Long id, ReceivableUpdateRequest r){
        var e=mapper.selectById(id);
        if(e==null) throw new BusinessException(R.CODE_NOT_FOUND,"应收单不存在");
        if(r.getCustomerName()!=null) e.setCustomerName(r.getCustomerName());
        if(r.getTotalAmount()!=null) e.setTotalAmount(r.getTotalAmount());
        if(r.getDueDate()!=null) e.setDueDate(r.getDueDate());
        if(r.getRemark()!=null) e.setRemark(r.getRemark());
        e.setStatus(deriveStatus(e.getTotalAmount(), e.getReceivedAmount()));
        mapper.updateById(e);
    }
    @Override @Transactional public void delete(Long id){mapper.deleteById(id);}

    @Override @Transactional public void confirmPayment(Long id, ConfirmPaymentRequest r){
        var e=mapper.selectById(id);
        if(e==null) throw new BusinessException(R.CODE_NOT_FOUND,"应收单不存在");
        var amt=r.getAmount()!=null?r.getAmount():BigDecimal.ZERO;
        if(amt.signum()<=0) throw new BusinessException(R.CODE_PARAM_INVALID,"确认金额必须大于0");
        var received=(e.getReceivedAmount()!=null?e.getReceivedAmount():BigDecimal.ZERO).add(amt);
        if(received.compareTo(e.getTotalAmount())>0) throw new BusinessException(R.CODE_PARAM_INVALID,"已收金额超过应收总额");
        e.setReceivedAmount(received);
        e.setStatus(deriveStatus(e.getTotalAmount(), received));
        if(r.getRemark()!=null) e.setRemark(r.getRemark());
        mapper.updateById(e);
    }

    @Override @Transactional public int batchConfirm(List<Long> ids){
        if(ids==null||ids.isEmpty()) return 0;
        int n=0;
        for(Long id:ids){
            var e=mapper.selectById(id);
            if(e==null) continue;
            e.setReceivedAmount(e.getTotalAmount());
            e.setStatus("received");
            mapper.updateById(e); n++;
        }
        return n;
    }

    @Override public AgingReportVO aging(){
        var list=mapper.selectList(new LambdaQueryWrapper<FinReceivable>()
            .eq(FinReceivable::getDeleted,0).ne(FinReceivable::getStatus,"received"));
        return buildAging(list);
    }

    /** 状态推导：已收>=总额→received；0<已收<总额→partial；否则 pending（逾期由账龄体现） */
    static String deriveStatus(BigDecimal total, BigDecimal received){
        var r=received!=null?received:BigDecimal.ZERO;
        if(r.compareTo(total)>=0) return "received";
        if(r.signum()>0) return "partial";
        return "pending";
    }

    private AgingReportVO buildAging(List<FinReceivable> list){
        int[][] ranges={{0,0},{1,30},{31,60},{61,90},{91,-1}};
        String[] labels={"未到期","1-30天","31-60天","61-90天","90天以上"};
        var buckets=new ArrayList<AgingReportVO.Bucket>();
        var amounts=new BigDecimal[ranges.length]; var counts=new long[ranges.length];
        for(int i=0;i<ranges.length;i++) amounts[i]=BigDecimal.ZERO;
        var total=BigDecimal.ZERO; var today=LocalDate.now();
        for(var e:list){
            var bal=e.getTotalAmount().subtract(e.getReceivedAmount()!=null?e.getReceivedAmount():BigDecimal.ZERO);
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
}