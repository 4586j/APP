package com.erp.user.controller;
import com.erp.common.dto.BatchImportResult;
import com.erp.common.model.R;
import com.erp.user.dto.*;
import com.erp.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/system/users")
@RequiredArgsConstructor
@Tag(name = "用户管理")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "erp.user.persistence", havingValue = "mysql", matchIfMissing = true)
public class UserController {
    private final UserService userService;
    @GetMapping @PreAuthorize("hasAuthority('user:view')") public R<?> page(UserQuery query) { return R.ok(userService.pageUsers(query)); }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('user:view')") public R<UserVO> get(@PathVariable Long id) { return R.ok(userService.getUserById(id)); }
    @PostMapping @PreAuthorize("hasAuthority('user:create')") public R<Long> create(@Valid @RequestBody UserCreateRequest req) { return R.ok(userService.createUser(req)); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('user:update')") public R<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest req) { userService.updateUser(id, req); return R.ok(); }
    @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('user:delete')") public R<Void> delete(@PathVariable Long id) { userService.deleteUser(id); return R.ok(); }
    @PutMapping("/{id}/lock") @PreAuthorize("hasAuthority('user:update')") public R<Void> lock(@PathVariable Long id) { userService.lockUser(id); return R.ok(); }
    @PutMapping("/{id}/unlock") @PreAuthorize("hasAuthority('user:update')") public R<Void> unlock(@PathVariable Long id) { userService.unlockUser(id); return R.ok(); }
    @PutMapping("/{id}/reset-password") @PreAuthorize("hasAuthority('user:update')") public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest req) { userService.resetPassword(id, req.getNewPassword()); return R.ok(); }

    @PostMapping("/batch-reset-password")
    @PreAuthorize("hasAuthority('user:update')")
    public R<Void> batchResetPassword(@Valid @RequestBody BatchResetPasswordRequest req) {
        userService.batchResetPassword(req.getUserIds(), req.getNewPassword());
        return R.ok();
    }

    @PutMapping("/{id}/roles") @PreAuthorize("hasAuthority('user:assign-role')") public R<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRolesRequest req) { userService.assignRoles(id, req.getRoleIds()); return R.ok(); }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('user:create')")
    public R<BatchImportResult> batchCreate(@Valid @RequestBody List<UserCreateRequest> list) {
        return R.ok(userService.batchCreateUsers(list));
    }

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('user:create')")
    public R<BatchImportResult> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return R.ok(userService.importUsersFromExcel(file.getInputStream()));
    }

    @GetMapping("/import-template")
    @PreAuthorize("hasAuthority('user:view')")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("用户导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        com.alibaba.excel.EasyExcel.write(response.getOutputStream(), UserImportExcelDTO.class)
                .sheet("用户模板")
                .doWrite(List.of());
    }
}