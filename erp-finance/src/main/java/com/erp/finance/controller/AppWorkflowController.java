package com.erp.finance.controller;
import com.erp.common.model.R;
import com.erp.finance.dto.*;
import com.erp.finance.service.AppWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/v1/workflows") @RequiredArgsConstructor
public class AppWorkflowController {
    final AppWorkflowService workflowService;
    @GetMapping @PreAuthorize("hasAuthority('approval:workflow:view')") public R<List<WorkflowVO>> list(){return R.ok(workflowService.list());}
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('approval:workflow:view')") public R<WorkflowVO> get(@PathVariable Long id){return R.ok(workflowService.getById(id));}
    @PostMapping @PreAuthorize("hasAuthority('approval:workflow:manage')") public R<Long> create(@Valid @RequestBody WorkflowSaveRequest r){return R.ok(workflowService.create(r));}
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('approval:workflow:manage')") public R<Void> update(@PathVariable Long id, @Valid @RequestBody WorkflowSaveRequest r){workflowService.update(id,r);return R.ok();}
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('approval:workflow:manage')") public R<Void> delete(@PathVariable Long id){workflowService.delete(id);return R.ok();}
}
