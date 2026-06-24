package com.erp.user.controller;
import com.erp.common.model.R;
import com.erp.user.dto.*;
import com.erp.user.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/system/roles")
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "erp.user.persistence", havingValue = "mysql", matchIfMissing = true)
public class RoleController {
    private final RoleService roleService;
    @GetMapping @PreAuthorize("hasAuthority('role:view')") public R<List<RoleVO>> list() { return R.ok(roleService.listAll()); }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('role:view')") public R<RoleVO> get(@PathVariable Long id) { return R.ok(roleService.getById(id)); }
    @PostMapping @PreAuthorize("hasAuthority('role:create')") public R<Long> create(@Valid @RequestBody RoleCreateRequest req) { return R.ok(roleService.create(req)); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('role:update')") public R<Void> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest req) { roleService.update(id, req); return R.ok(); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('role:delete')") public R<Void> delete(@PathVariable Long id) { roleService.delete(id); return R.ok(); }
    @PutMapping("/{id}/permissions") @PreAuthorize("hasAuthority('role:assign-perm')") public R<Void> assignPermissions(@PathVariable Long id, @Valid @RequestBody AssignPermissionsRequest req) { roleService.assignPermissions(id, req.getPermissionIds()); return R.ok(); }
}