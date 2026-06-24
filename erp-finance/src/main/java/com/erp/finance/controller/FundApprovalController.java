package com.erp.finance.controller;
import com.erp.common.model.R;
import com.erp.finance.dto.*;
import com.erp.finance.service.FinFundApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/fund-approvals") @RequiredArgsConstructor
public class FundApprovalController {
    final FinFundApprovalService approvalService;
    @GetMapping public R<FundApprovalPageVO> list(FundApprovalQuery q){return R.ok(approvalService.listPage(q));}
    @PostMapping public R<Long> create(@Valid @RequestBody FundApprovalCreateRequest r){return R.ok(approvalService.create(r,1L));}
    @PutMapping("/{id}/approve") public R<Void> approve(@PathVariable Long id){approvalService.approve(id,1L);return R.ok();}
    @PutMapping("/{id}/reject") public R<Void> reject(@PathVariable Long id, @RequestBody String comment){approvalService.reject(id,comment,1L);return R.ok();}
}