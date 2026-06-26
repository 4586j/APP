package com.erp.product.controller;

import com.erp.common.model.R;
import com.erp.product.dto.PrdCategoryVO;
import com.erp.product.dto.CategoryCreateRequest;
import com.erp.product.service.PrdCategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "产品分类")
public class CategoryController {
    final PrdCategoryService categoryService;

    @GetMapping("/tree")
    public R<List<PrdCategoryVO>> tree() {
        return R.ok(categoryService.listTree());
    }

    @GetMapping("/{id}")
    public R<PrdCategoryVO> get(@PathVariable Long id) {
        return R.ok(categoryService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:create')")
    public R<Long> create(@Valid @RequestBody CategoryCreateRequest req) {
        return R.ok(categoryService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:update')")
    public R<Void> update(@PathVariable Long id, @RequestBody CategoryCreateRequest req) {
        categoryService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:delete')")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }
}
