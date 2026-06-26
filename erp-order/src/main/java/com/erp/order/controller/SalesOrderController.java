package com.erp.order.controller;
import com.erp.common.model.R;
import com.erp.order.dto.*;
import com.erp.order.service.OrdSalesOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/sales-orders") @RequiredArgsConstructor
@Tag(name = "销售订单")
public class SalesOrderController {
    final OrdSalesOrderService salesOrderService;
    @GetMapping public R<SalesOrderPageVO> list(SalesOrderQuery q) { return R.ok(salesOrderService.listPage(q)); }
    @GetMapping("/{id}") public R<SalesOrderVO> get(@PathVariable Long id) { return R.ok(salesOrderService.getById(id)); }
    @PostMapping @PreAuthorize("hasAuthority('order:create')") public R<SalesOrderVO> create(@Valid @RequestBody SalesOrderCreateRequest r) { return R.ok(salesOrderService.create(r, 1L)); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('order:update')") public R<Void> update(@PathVariable Long id, @RequestBody SalesOrderUpdateRequest r) { salesOrderService.update(id,r); return R.ok(); }
    @PutMapping("/{id}/status") @PreAuthorize("hasAuthority('order:update')") public R<SalesOrderVO> changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest r) { return R.ok(salesOrderService.changeStatus(id, r, 1L)); }
    @GetMapping("/{id}/profit") public R<OrderProfitVO> profit(@PathVariable Long id) { return R.ok(salesOrderService.calculateProfit(id)); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('order:delete')") public R<Void> delete(@PathVariable Long id) { salesOrderService.delete(id); return R.ok(); }
}