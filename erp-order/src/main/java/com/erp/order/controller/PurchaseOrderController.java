package com.erp.order.controller;
import com.erp.common.model.R;
import com.erp.order.dto.*;
import com.erp.order.service.OrdPurchaseOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/purchase-orders") @RequiredArgsConstructor
@Tag(name = "采购订单")
public class PurchaseOrderController {
    final OrdPurchaseOrderService purchaseOrderService;
    @GetMapping public R<PurchaseOrderPageVO> list(PurchaseOrderQuery q) { return R.ok(purchaseOrderService.listPage(q)); }
    @GetMapping("/{id}") public R<PurchaseOrderVO> get(@PathVariable Long id) { return R.ok(purchaseOrderService.getById(id)); }
    @PostMapping @PreAuthorize("hasAuthority('order:create')") public R<PurchaseOrderVO> create(@Valid @RequestBody PurchaseOrderCreateRequest r) { return R.ok(purchaseOrderService.create(r, 1L)); }
    @PutMapping("/{id}/status") @PreAuthorize("hasAuthority('order:update')") public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest r) { purchaseOrderService.updateStatus(id,r,1L); return R.ok(); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('order:delete')") public R<Void> delete(@PathVariable Long id) { purchaseOrderService.delete(id); return R.ok(); }
}