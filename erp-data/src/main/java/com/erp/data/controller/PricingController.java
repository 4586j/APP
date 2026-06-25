package com.erp.data.controller;
import com.erp.common.model.R; import com.erp.data.dto.PricingPageVO;
import com.erp.data.dto.PricingQuery; import com.erp.data.dto.PricingVO;
import com.erp.data.dto.PricingCreateRequest;
import com.erp.data.service.PricingService;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/data/pricing") @RequiredArgsConstructor
public class PricingController {
    final PricingService service;
    @GetMapping public R<PricingPageVO> list(PricingQuery q){return R.ok(service.listPage(q));}
    @GetMapping("/{id}") public R<PricingVO> get(@PathVariable Long id){return R.ok(service.getById(id));}
    @PostMapping public R<Long> create(@Valid @RequestBody PricingCreateRequest r){return R.ok(service.create(r));}
    @PutMapping("/{id}") public R<Void> update(@PathVariable Long id,@Valid @RequestBody PricingCreateRequest r){service.update(id,r);return R.ok();}
    @DeleteMapping("/{id}") public R<Void> delete(@PathVariable Long id){service.delete(id);return R.ok();}
}