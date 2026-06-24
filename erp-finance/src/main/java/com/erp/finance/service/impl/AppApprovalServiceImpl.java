package com.erp.finance.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.finance.dto.*;
import com.erp.finance.entity.*;
import com.erp.finance.mapper.*;
import com.erp.finance.service.AppApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service @RequiredArgsConstructor
public class AppApprovalServiceImpl implements AppApprovalService {
    final AppApprovalRequestMapper requestMapper;
    final AppApprovalHistoryMapper historyMapper;
    final AppWorkflowMapper workflowMapper;
    final AppWorkflowNodeMapper nodeMapper;

    @Override
    public AppApprovalPageVO listPage(AppApprovalRequestQuery q) {
        if (q.getPage()==null||q.getPage()<1) q.setPage(1);
        if (q.getSize()==null||q.getSize()<1) q.setSize(20);
        var w = new LambdaQueryWrapper<AppApprovalRequest>();
        if (q.getTargetType()!=null) w.eq(AppApprovalRequest::getTargetType, q.getTargetType());
        if (q.getStatus()!=null) w.eq(AppApprovalRequest::getStatus, q.getStatus());
        if (q.getApplicant()!=null) w.eq(AppApprovalRequest::getApplicant, q.getApplicant());
        w.orderByDesc(AppApprovalRequest::getCreatedAt);
        var p = requestMapper.selectPage(new Page<>(q.getPage(), Math.min(q.getSize(),100)), w);
        var records = p.getRecords().stream().map(r -> {
            var hw = new LambdaQueryWrapper<AppApprovalHistory>();
            hw.eq(AppApprovalHistory::getRequestId, r.getId()).orderByAsc(AppApprovalHistory::getCreatedAt);
            var history = historyMapper.selectList(hw).stream().map(h ->
                AppApprovalHistoryVO.builder().id(h.getId()).requestId(h.getRequestId())
                    .approver(h.getApprover()).action(h.getAction()).comment(h.getComment()).createdAt(h.getCreatedAt()).build()
            ).toList();
            return AppApprovalRequestVO.builder().id(r.getId()).requestNo(r.getRequestNo())
                .workflowId(r.getWorkflowId()).targetType(r.getTargetType()).targetId(r.getTargetId())
                .title(r.getTitle()).amount(r.getAmount()).currency(r.getCurrency())
                .status(r.getStatus()).applicant(r.getApplicant()).createdAt(r.getCreatedAt())
                .history(history).build();
        }).toList();
        return AppApprovalPageVO.builder().records(records).total(p.getTotal()).size(p.getSize()).current(p.getCurrent()).build();
    }

    @Override
    public AppApprovalRequestVO getById(Long id) {
        var r = requestMapper.selectById(id);
        if (r == null) return null;
        var hw = new LambdaQueryWrapper<AppApprovalHistory>();
        hw.eq(AppApprovalHistory::getRequestId, id).orderByAsc(AppApprovalHistory::getCreatedAt);
        var history = historyMapper.selectList(hw).stream().map(h ->
            AppApprovalHistoryVO.builder().id(h.getId()).requestId(h.getRequestId())
                .approver(h.getApprover()).action(h.getAction()).comment(h.getComment()).createdAt(h.getCreatedAt()).build()
        ).toList();
        return AppApprovalRequestVO.builder().id(r.getId()).requestNo(r.getRequestNo())
            .workflowId(r.getWorkflowId()).targetType(r.getTargetType()).targetId(r.getTargetId())
            .title(r.getTitle()).amount(r.getAmount()).currency(r.getCurrency())
            .status(r.getStatus()).applicant(r.getApplicant()).createdAt(r.getCreatedAt())
            .history(history).build();
    }

    @Override
    @Transactional
    public Long createRequest(String targetType, Long targetId, String title, BigDecimal amount, Long applicant) {
        // 按 target_type 找流程
        var ww = new LambdaQueryWrapper<AppWorkflow>();
        ww.eq(AppWorkflow::getTargetType, targetType).eq(AppWorkflow::getStatus, 1).last("LIMIT 1");
        var wf = workflowMapper.selectOne(ww);

        // 生成审批编号
        var datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var reqNo = "AR-" + datePart + "-" + System.currentTimeMillis() % 100000;

        var r = new AppApprovalRequest();
        r.setRequestNo(reqNo);
        r.setTitle(title);
        r.setTargetType(targetType);
        r.setTargetId(targetId);
        r.setAmount(amount);
        r.setApplicant(applicant);
        if (wf != null) r.setWorkflowId(wf.getId());
        requestMapper.insert(r);
        return r.getId();
    }

    @Override
    @Transactional
    public void approve(Long id, Long approver, String comment) {
        var r = requestMapper.selectById(id);
        if (r == null) throw new RuntimeException("审批请求不存在");
        if (!"pending".equals(r.getStatus())) throw new RuntimeException("审批已处理");
        r.setStatus("approved");
        requestMapper.updateById(r);
        var h = new AppApprovalHistory();
        h.setRequestId(id); h.setApprover(approver); h.setAction("approved"); h.setComment(comment);
        historyMapper.insert(h);
    }

    @Override
    @Transactional
    public void reject(Long id, Long approver, String comment) {
        var r = requestMapper.selectById(id);
        if (r == null) throw new RuntimeException("审批请求不存在");
        if (!"pending".equals(r.getStatus())) throw new RuntimeException("审批已处理");
        r.setStatus("rejected");
        requestMapper.updateById(r);
        var h = new AppApprovalHistory();
        h.setRequestId(id); h.setApprover(approver); h.setAction("rejected"); h.setComment(comment);
        historyMapper.insert(h);
    }
}