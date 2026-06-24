package com.erp.product.controller;
import com.erp.common.model.R;
import com.erp.product.dto.*;
import com.erp.product.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/products") @RequiredArgsConstructor
public class ProductController {
    final PrdProductService productService;
    @GetMapping public R<ProductPageVO> list(ProductQuery q){ return R.ok(productService.listPage(q)); }
    @GetMapping("/{id}") public R<ProductVO> get(@PathVariable Long id){ return R.ok(productService.getById(id)); }
    @PostMapping @PreAuthorize("hasAuthority('product:create')") public R<Long> create(@Valid @RequestBody ProductCreateRequest r){ return R.ok(productService.create(r)); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('product:update')") public R<Void> update(@PathVariable Long id,@RequestBody ProductCreateRequest r){ productService.update(id,r); return R.ok(); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('product:delete')") public R<Void> delete(@PathVariable Long id){ productService.delete(id); return R.ok(); }
}