package com.erp.customer.controller;
import com.erp.common.model.R;
import com.erp.customer.dto.*;
import com.erp.customer.service.CustSupplierService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/suppliers") @RequiredArgsConstructor
@Tag(name = "供应商管理")
public class SupplierController {
    final CustSupplierService supplierService;
    @GetMapping public R<SupplierPageVO> list(SupplierQuery q) { return R.ok(supplierService.listPage(q)); }
    @GetMapping("/{id}") public R<SupplierVO> get(@PathVariable Long id) { return R.ok(supplierService.getById(id)); }
    @PostMapping @PreAuthorize("hasAuthority('supplier:create')") public R<Long> create(@Valid @RequestBody SupplierCreateRequest r) { return R.ok(supplierService.create(r)); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('supplier:update')") public R<Void> update(@PathVariable Long id, @RequestBody SupplierCreateRequest r) { supplierService.update(id,r); return R.ok(); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('supplier:delete')") public R<Void> delete(@PathVariable Long id) { supplierService.delete(id); return R.ok(); }
}