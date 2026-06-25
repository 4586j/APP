package com.erp.finance.controller;
import com.erp.common.model.R;
import com.erp.finance.dto.*;
import com.erp.finance.service.FinExchangeRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/exchange-rates") @RequiredArgsConstructor
public class ExchangeRateController {
    final FinExchangeRateService rateService;
    @GetMapping @PreAuthorize("hasAuthority('finance:exchange-rate:view')") public R<ExchangeRatePageVO> list(ExchangeRateQuery q){return R.ok(rateService.listPage(q));}
    @GetMapping("/current") @PreAuthorize("hasAuthority('finance:exchange-rate:view')") public R<ExchangeRateVO> current(@RequestParam String from, @RequestParam String to){return R.ok(rateService.getCurrent(from,to));}
    @PostMapping @PreAuthorize("hasAuthority('finance:exchange-rate:create')") public R<Long> create(@Valid @RequestBody ExchangeRateCreateRequest r){return R.ok(rateService.create(r));}
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('finance:exchange-rate:delete')") public R<Void> delete(@PathVariable Long id){rateService.delete(id);return R.ok();}
}