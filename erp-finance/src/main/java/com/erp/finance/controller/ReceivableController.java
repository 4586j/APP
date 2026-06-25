package com.erp.finance.controller;
import com.erp.common.model.R; import com.erp.finance.dto.*; import com.erp.finance.service.FinReceivableService;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/receivables") @RequiredArgsConstructor
public class ReceivableController {
    final FinReceivableService service;
    @GetMapping @PreAuthorize("hasAuthority('finance:receivable:view')") public R<ReceivablePageVO> list(ReceivableQuery q){return R.ok(service.listPage(q));}
    @GetMapping("/aging") @PreAuthorize("hasAuthority('finance:receivable:view')") public R<AgingReportVO> aging(){return R.ok(service.aging());}
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('finance:receivable:view')") public R<ReceivableVO> get(@PathVariable Long id){return R.ok(service.getById(id));}
    @PostMapping @PreAuthorize("hasAuthority('finance:receivable:create')") public R<Long> create(@Valid @RequestBody ReceivableCreateRequest r){return R.ok(service.create(r));}
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('finance:receivable:update')") public R<Void> update(@PathVariable Long id, @RequestBody ReceivableUpdateRequest r){service.update(id,r);return R.ok();}
    @PutMapping("/{id}/confirm-payment") @PreAuthorize("hasAuthority('finance:receivable:confirm')") public R<Void> confirm(@PathVariable Long id, @Valid @RequestBody ConfirmPaymentRequest r){service.confirmPayment(id,r);return R.ok();}
    @PutMapping("/batch-confirm") @PreAuthorize("hasAuthority('finance:receivable:confirm')") public R<Integer> batchConfirm(@Valid @RequestBody BatchConfirmRequest r){return R.ok(service.batchConfirm(r.getIds()));}
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('finance:receivable:delete')") public R<Void> delete(@PathVariable Long id){service.delete(id);return R.ok();}
}