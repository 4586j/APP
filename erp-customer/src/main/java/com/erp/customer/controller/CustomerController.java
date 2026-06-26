package com.erp.customer.controller;
import com.erp.common.model.R;
import com.erp.customer.dto.*;
import com.erp.customer.service.CustCustomerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/customers") @RequiredArgsConstructor
@Tag(name = "客户管理")
public class CustomerController {
    final CustCustomerService customerService;
    @GetMapping public R<CustomerPageVO> list(CustomerQuery q) { return R.ok(customerService.listPage(q)); }
    @GetMapping("/{id}") public R<CustomerVO> get(@PathVariable Long id) { return R.ok(customerService.getById(id)); }
    @PostMapping @PreAuthorize("hasAuthority('customer:create')") public R<Long> create(@Valid @RequestBody CustomerCreateRequest r) { return R.ok(customerService.create(r)); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('customer:update')") public R<Void> update(@PathVariable Long id, @RequestBody CustomerCreateRequest r) { customerService.update(id,r); return R.ok(); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('customer:delete')") public R<Void> delete(@PathVariable Long id) { customerService.delete(id); return R.ok(); }
}