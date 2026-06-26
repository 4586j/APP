package com.erp.finance.controller;
import com.erp.common.model.R;
import com.erp.finance.dto.*;
import com.erp.finance.service.FinFundApprovalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/fund-approvals") @RequiredArgsConstructor
@Tag(name = "资金审批")
public class FundApprovalController {
    final FinFundApprovalService approvalService;
    @GetMapping @PreAuthorize("hasAuthority('finance:fund:view')") public R<FundApprovalPageVO> list(FundApprovalQuery q){return R.ok(approvalService.listPage(q));}
    @GetMapping("/my-pending") @PreAuthorize("hasAuthority('finance:fund:approve')") public R<FundApprovalPageVO> myPending(@RequestParam(required=false) String approverRole, @RequestParam(required=false) Integer page, @RequestParam(required=false) Integer size){return R.ok(approvalService.myPending(approverRole,page,size));}
    @PostMapping @PreAuthorize("hasAuthority('finance:fund:create')") public R<Long> create(@Valid @RequestBody FundApprovalCreateRequest r){return R.ok(approvalService.create(r,1L));}
    @PutMapping("/{id}/approve") @PreAuthorize("hasAuthority('finance:fund:approve')") public R<Void> approve(@PathVariable Long id, @RequestBody(required=false) String comment){approvalService.approve(id,1L,comment);return R.ok();}
    @PutMapping("/{id}/reject") @PreAuthorize("hasAuthority('finance:fund:approve')") public R<Void> reject(@PathVariable Long id, @RequestBody(required=false) String comment){approvalService.reject(id,comment,1L);return R.ok();}
}