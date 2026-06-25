package com.erp.finance.controller;
import com.erp.common.model.R; import com.erp.finance.dto.*; import com.erp.finance.service.FinReceivableService;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/receivables") @RequiredArgsConstructor
public class ReceivableController {
    final FinReceivableService service;
    @GetMapping public R<ReceivablePageVO> list(ReceivableQuery q){return R.ok(service.listPage(q));}
    @GetMapping("/{id}") public R<ReceivableVO> get(@PathVariable Long id){return R.ok(service.getById(id));}
    @PostMapping public R<Long> create(@Valid @RequestBody ReceivableCreateRequest r){return R.ok(service.create(r));}
    @DeleteMapping("/{id}") public R<Void> delete(@PathVariable Long id){service.delete(id);return R.ok();}
}