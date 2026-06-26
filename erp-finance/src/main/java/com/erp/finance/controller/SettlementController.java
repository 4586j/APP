package com.erp.finance.controller;
import com.erp.common.model.R; import com.erp.finance.dto.*; import com.erp.finance.service.FinSettlementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/settlements") @RequiredArgsConstructor
@Tag(name = "结算管理")
public class SettlementController {
    final FinSettlementService service;
    @GetMapping public R<SettlementPageVO> list(SettlementQuery q){return R.ok(service.listPage(q));}
    @GetMapping("/{id}") public R<SettlementVO> get(@PathVariable Long id){return R.ok(service.getById(id));}
    @PostMapping public R<Long> create(@Valid @RequestBody SettlementCreateRequest r){return R.ok(service.create(r));}
    @DeleteMapping("/{id}") public R<Void> delete(@PathVariable Long id){service.delete(id);return R.ok();}
}