package com.erp.finance.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.finance.dto.*;
import com.erp.finance.entity.FinFundApproval;
import com.erp.finance.mapper.FinFundApprovalMapper;
import com.erp.finance.service.FinFundApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate; import java.time.format.DateTimeFormatter;
@Service @RequiredArgsConstructor
public class FinFundApprovalServiceImpl implements FinFundApprovalService {
    final FinFundApprovalMapper approvalMapper;
    FundApprovalVO toVO(FinFundApproval x){
        return FundApprovalVO.builder().id(x.getId()).requestNo(x.getRequestNo())
            .title(x.getTitle()).fundType(x.getFundType()).amount(x.getAmount())
            .currency(x.getCurrency()).supplierId(x.getSupplierId())
            .description(x.getDescription()).status(x.getStatus())
            .applicant(x.getApplicant()).createdAt(x.getCreatedAt()).build();
    }
    public FundApprovalPageVO listPage(FundApprovalQuery q){
        if (q.getPage()==null||q.getPage()<1)q.setPage(1);
        if (q.getSize()==null||q.getSize()<1)q.setSize(20);
        var w=new LambdaQueryWrapper<FinFundApproval>();
        if (q.getRequestNo()!=null) w.like(FinFundApproval::getRequestNo,q.getRequestNo());
        if (q.getFundType()!=null) w.eq(FinFundApproval::getFundType,q.getFundType());
        if (q.getStatus()!=null) w.eq(FinFundApproval::getStatus,q.getStatus());
        w.orderByDesc(FinFundApproval::getCreatedAt);
        var p=approvalMapper.selectPage(new Page<>(q.getPage(),Math.min(q.getSize(),100)),w);
        return FundApprovalPageVO.builder().records(p.getRecords().stream().map(this::toVO).toList())
            .total(p.getTotal()).size(p.getSize()).current(p.getCurrent()).build();
    }
    @Transactional public Long create(FundApprovalCreateRequest r, Long userId){
        var dateStr=LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var seq=approvalMapper.selectCount(new LambdaQueryWrapper<FinFundApproval>()
            .like(FinFundApproval::getRequestNo,"F-"+dateStr))+1;
        var a=new FinFundApproval(); a.setRequestNo(String.format("F-%s-%04d",dateStr,seq));
        a.setTitle(r.getTitle()); a.setFundType(r.getFundType());
        a.setAmount(r.getAmount()); a.setCurrency(r.getCurrency()!=null?r.getCurrency():"CNY");
        a.setSupplierId(r.getSupplierId()); a.setDescription(r.getDescription());
        a.setStatus("pending"); a.setApplicant(userId);
        approvalMapper.insert(a); return a.getId();
    }
    @Transactional public void approve(Long id, Long userId){
        var a=approvalMapper.selectById(id);
        if(a==null)throw new BusinessException(R.CODE_NOT_FOUND,"approval not found");
        a.setStatus("approved"); approvalMapper.updateById(a);
    }
    @Transactional public void reject(Long id, String comment, Long userId){
        var a=approvalMapper.selectById(id);
        if(a==null)throw new BusinessException(R.CODE_NOT_FOUND,"approval not found");
        a.setStatus("rejected"); approvalMapper.updateById(a);
    }
}