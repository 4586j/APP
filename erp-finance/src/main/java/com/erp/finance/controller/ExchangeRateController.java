package com.erp.finance.controller;
import com.erp.common.model.R;
import com.erp.finance.dto.*;
import com.erp.finance.service.FinExchangeRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/exchange-rates") @RequiredArgsConstructor
public class ExchangeRateController {
    final FinExchangeRateService rateService;
    @GetMapping public R<ExchangeRatePageVO> list(ExchangeRateQuery q){return R.ok(rateService.listPage(q));}
    @PostMapping public R<Long> create(@Valid @RequestBody ExchangeRateCreateRequest r){return R.ok(rateService.create(r));}
    @DeleteMapping("/{id}") public R<Void> delete(@PathVariable Long id){rateService.delete(id);return R.ok();}
}