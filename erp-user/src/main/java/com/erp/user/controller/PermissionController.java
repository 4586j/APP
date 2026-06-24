package com.erp.user.controller;
import com.erp.common.model.R;
import com.erp.user.dto.*;
import com.erp.user.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/system/permissions")
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "erp.user.persistence", havingValue = "mysql", matchIfMissing = true)
public class PermissionController {
    private final PermissionService permissionService;
    @GetMapping("/tree") @PreAuthorize("hasAuthority('permission:view')") public R<List<PermissionTreeNode>> tree() { return R.ok(permissionService.treeAll()); }
    @GetMapping @PreAuthorize("hasAuthority('permission:view')") public R<List<PermissionVO>> list() { return R.ok(permissionService.listAll()); }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('permission:view')") public R<PermissionVO> get(@PathVariable Long id) { return R.ok(permissionService.getById(id)); }
    @PostMapping @PreAuthorize("hasAuthority('permission:create')") public R<Long> create(@Valid @RequestBody PermissionCreateRequest req) { return R.ok(permissionService.create(req)); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('permission:update')") public R<Void> update(@PathVariable Long id, @Valid @RequestBody PermissionUpdateRequest req) { permissionService.update(id, req); return R.ok(); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('permission:delete')") public R<Void> delete(@PathVariable Long id) { permissionService.delete(id); return R.ok(); }
}