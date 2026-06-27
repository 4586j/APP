package com.erp.user.controller;
import com.erp.common.model.R;
import com.erp.user.dto.AssignPermissionsRequest;
import com.erp.user.dto.DepartmentCreateRequest;
import com.erp.user.dto.DepartmentOption;
import com.erp.user.dto.DepartmentTreeNode;
import com.erp.user.dto.DepartmentUpdateRequest;
import com.erp.user.service.DeptUserPermissionService;
import com.erp.user.service.DepartmentService;
import com.erp.security.annotation.CurrentUser;
import com.erp.security.user.LoginUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/system/departments")
@RequiredArgsConstructor
@Tag(name = "部门管理")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "erp.user.persistence", havingValue = "mysql", matchIfMissing = true)
public class DepartmentController {
    private final DepartmentService departmentService;
    private final DeptUserPermissionService deptUserPermissionService;
    @GetMapping @PreAuthorize("hasAuthority('department:view')") public R<List<DepartmentTreeNode>> list() { return R.ok(departmentService.treeAll()); }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('department:view')") public R<DepartmentTreeNode> get(@PathVariable Long id) { return R.ok(departmentService.getById(id)); }
    @PostMapping @PreAuthorize("hasAuthority('department:create')") public R<Long> create(@Valid @RequestBody DepartmentCreateRequest req) { return R.ok(departmentService.create(req)); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('department:update')") public R<Void> update(@PathVariable Long id, @Valid @RequestBody DepartmentUpdateRequest req) { departmentService.update(id, req); return R.ok(); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('department:delete')") public R<Void> delete(@PathVariable Long id) { departmentService.delete(id); return R.ok(); }

    @GetMapping("/select-options")
    @PreAuthorize("hasAuthority('department:view')")
    public R<List<DepartmentOption>> selectOptions(@RequestParam(required = false) String keyword) {
        return R.ok(departmentService.selectOptions(keyword));
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('department:view')")
    public R<List<Long>> getPermissionIds(@PathVariable Long id) {
        return R.ok(departmentService.getPermissionIdsByDeptId(id));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('department:update')")
    public R<Void> assignPermissions(@PathVariable Long id, @Valid @RequestBody AssignPermissionsRequest req) {
        departmentService.assignPermissions(id, req.getPermissionIds());
        return R.ok();
    }

    // ========== 部门用户权限（部长为部门内用户分配权限） ==========

    /**
     * 获取某部门下某用户的权限 ID 列表。
     */
    @GetMapping("/{deptId}/users/{userId}/permissions")
    @PreAuthorize("hasAuthority('department:view')")
    public R<List<Long>> getUserPermissionIds(@PathVariable Long deptId, @PathVariable Long userId) {
        return R.ok(deptUserPermissionService.getPermissionIdsByUserAndDept(userId, deptId));
    }

    /**
     * 为某部门下的用户分配权限（覆盖式），需要部长/管理员权限。
     */
    @PutMapping("/{deptId}/users/{userId}/permissions")
    @PreAuthorize("hasAuthority('department:update')")
    public R<Void> assignUserPermissions(@PathVariable Long deptId,
                                          @PathVariable Long userId,
                                          @Valid @RequestBody AssignPermissionsRequest req,
                                          @CurrentUser LoginUser currentUser) {
        deptUserPermissionService.assignPermissions(userId, deptId, req.getPermissionIds(), currentUser.getId());
        return R.ok();
    }
}