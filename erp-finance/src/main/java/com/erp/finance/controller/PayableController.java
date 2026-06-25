package com.erp.finance.controller;
import com.erp.common.model.R; import com.erp.finance.dto.*; import com.erp.finance.service.FinPayableService;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/payables") @RequiredArgsConstructor
public class PayableController {
    final FinPayableService service;
    @GetMapping @PreAuthorize("hasAuthority('finance:payable:view')") public R<PayablePageVO> list(PayableQuery q){return R.ok(service.listPage(q));}
    @GetMapping("/aging") @PreAuthorize("hasAuthority('finance:payable:view')") public R<AgingReportVO> aging(){return R.ok(service.aging());}
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('finance:payable:view')") public R<PayableVO> get(@PathVariable Long id){return R.ok(service.getById(id));}
    @PostMapping @PreAuthorize("hasAuthority('finance:payable:create')") public R<Long> create(@Valid @RequestBody PayableCreateRequest r){return R.ok(service.create(r));}
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('finance:payable:update')") public R<Void> update(@PathVariable Long id, @RequestBody PayableUpdateRequest r){service.update(id,r);return R.ok();}
    @PutMapping("/{id}/confirm-payment") @PreAuthorize("hasAuthority('finance:payable:confirm')") public R<Void> confirm(@PathVariable Long id, @Valid @RequestBody ConfirmPaymentRequest r){service.confirmPayment(id,r);return R.ok();}
    @PutMapping("/batch-confirm") @PreAuthorize("hasAuthority('finance:payable:confirm')") public R<Integer> batchConfirm(@Valid @RequestBody BatchConfirmRequest r){return R.ok(service.batchConfirm(r.getIds()));}
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('finance:payable:delete')") public R<Void> delete(@PathVariable Long id){service.delete(id);return R.ok();}
}