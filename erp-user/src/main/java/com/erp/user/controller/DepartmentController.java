package com.erp.user.controller;
import com.erp.common.model.R;
import com.erp.user.dto.DepartmentCreateRequest;
import com.erp.user.dto.DepartmentTreeNode;
import com.erp.user.dto.DepartmentUpdateRequest;
import com.erp.user.service.DepartmentService;
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
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(javax.sql.DataSource.class)
public class DepartmentController {
    private final DepartmentService departmentService;
    @GetMapping @PreAuthorize("hasAuthority('department:view')") public R<List<DepartmentTreeNode>> list() { return R.ok(departmentService.treeAll()); }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('department:view')") public R<DepartmentTreeNode> get(@PathVariable Long id) { return R.ok(departmentService.getById(id)); }
    @PostMapping @PreAuthorize("hasAuthority('department:create')") public R<Long> create(@Valid @RequestBody DepartmentCreateRequest req) { return R.ok(departmentService.create(req)); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('department:update')") public R<Void> update(@PathVariable Long id, @Valid @RequestBody DepartmentUpdateRequest req) { departmentService.update(id, req); return R.ok(); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('department:delete')") public R<Void> delete(@PathVariable Long id) { departmentService.delete(id); return R.ok(); }
}