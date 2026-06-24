package com.erp.user.controller;
import com.erp.common.model.R;
import com.erp.user.dto.*;
import com.erp.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/system/users")
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(javax.sql.DataSource.class)
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
    @PutMapping("/{id}/roles") @PreAuthorize("hasAuthority('role:assign')") public R<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRolesRequest req) { userService.assignRoles(id, req.getRoleIds()); return R.ok(); }
}