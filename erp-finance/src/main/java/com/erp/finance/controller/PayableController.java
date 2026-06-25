package com.erp.finance.controller;
import com.erp.common.model.R; import com.erp.finance.dto.*; import com.erp.finance.service.FinPayableService;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/payables") @RequiredArgsConstructor
public class PayableController {
    final FinPayableService service;
    @GetMapping public R<PayablePageVO> list(PayableQuery q){return R.ok(service.listPage(q));}
    @GetMapping("/{id}") public R<PayableVO> get(@PathVariable Long id){return R.ok(service.getById(id));}
    @PostMapping public R<Long> create(@Valid @RequestBody PayableCreateRequest r){return R.ok(service.create(r));}
    @DeleteMapping("/{id}") public R<Void> delete(@PathVariable Long id){service.delete(id);return R.ok();}
}