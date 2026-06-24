package com.erp.finance.controller;
import com.erp.common.model.R;
import com.erp.finance.dto.*;
import com.erp.finance.service.AppApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/approvals") @RequiredArgsConstructor
public class AppApprovalController {
    final AppApprovalService approvalService;
    @GetMapping public R<AppApprovalPageVO> list(AppApprovalRequestQuery q){return R.ok(approvalService.listPage(q));}
    @GetMapping("/{id}") public R<AppApprovalRequestVO> get(@PathVariable Long id){return R.ok(approvalService.getById(id));}
    @PostMapping public R<Long> create(@Valid @RequestBody AppApprovalCreateRequest r){return R.ok(approvalService.createRequest(r.getTargetType(),r.getTargetId(),r.getTitle(),r.getAmount(),1L));}
    @PutMapping("/{id}/action") public R<Void> action(@PathVariable Long id, @Valid @RequestBody ApproveRequest r){
        if ("approved".equals(r.getAction())){approvalService.approve(id,1L,r.getComment());return R.ok();}
        if ("rejected".equals(r.getAction())){approvalService.reject(id,1L,r.getComment());return R.ok();}
        return R.fail(400,"无效操作: "+r.getAction());
    }
}