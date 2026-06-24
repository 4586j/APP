package com.erp.user.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.user.dto.*;
import com.erp.user.entity.*;
import com.erp.user.mapper.*;
import com.erp.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(javax.sql.DataSource.class)
public class UserServiceImpl implements UserService {
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private static final int MAX_LOGIN_FAILURES = 5;

    @Override public SysUser loadByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }
    @Override public List<String> getRolesByUserId(Long userId) { return roleMapper.selectRolesByUserId(userId); }
    @Override public List<String> getPermissionsByUserId(Long userId) { return permissionMapper.selectPermissionsByUserId(userId); }

    @Override public Page<?> pageUsers(UserQuery query) {
        Page<SysUser> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        if (query.getUsername() != null) w.like(SysUser::getUsername, query.getUsername());
        if (query.getRealName() != null) w.like(SysUser::getRealName, query.getRealName());
        if (query.getStatus() != null) w.eq(SysUser::getStatus, query.getStatus());
        w.orderByDesc(SysUser::getCreatedAt);
        return userMapper.selectPage(page, w);
    }

    @Override public UserVO getUserById(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(R.CODE_NOT_FOUND, "user not found");
        return UserVO.builder().id(u.getId()).username(u.getUsername()).realName(u.getRealName())
            .email(u.getEmail()).phone(u.getPhone()).departmentId(u.getDepartmentId())
            .status(u.getStatus()).lastLoginTime(u.getLastLoginTime()).createdAt(u.getCreatedAt()).build();
    }

    @Override @Transactional(rollbackFor = Exception.class) public Long createUser(UserCreateRequest req) {
        if (loadByUsername(req.getUsername()) != null) throw new BusinessException(R.CODE_PARAM_INVALID, "username exists");
        SysUser u = new SysUser(); u.setUsername(req.getUsername());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setRealName(req.getRealName()); u.setEmail(req.getEmail()); u.setPhone(req.getPhone());
        u.setDepartmentId(req.getDepartmentId()); u.setStatus(1);
        userMapper.insert(u); return u.getId();
    }
    @Override @Transactional(rollbackFor = Exception.class) public void updateUser(Long id, UserUpdateRequest req) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(R.CODE_NOT_FOUND, "user not found");
        if (req.getRealName() != null) u.setRealName(req.getRealName());
        if (req.getEmail() != null) u.setEmail(req.getEmail());
        if (req.getPhone() != null) u.setPhone(req.getPhone());
        if (req.getDepartmentId() != null) u.setDepartmentId(req.getDepartmentId());
        userMapper.updateById(u);
    }
    @Override @Transactional(rollbackFor = Exception.class) public void deleteUser(Long id) {
        if (userMapper.selectById(id) == null) throw new BusinessException(R.CODE_NOT_FOUND, "user not found");
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }
    @Override @Transactional(rollbackFor = Exception.class) public void lockUser(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(R.CODE_NOT_FOUND, "user not found");
        u.setStatus(0); userMapper.updateById(u);
    }
    @Override @Transactional(rollbackFor = Exception.class) public void unlockUser(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(R.CODE_NOT_FOUND, "user not found");
        u.setStatus(1); u.setLoginFailCount(0); u.setLockedUntil(null); userMapper.updateById(u);
    }
    @Override @Transactional(rollbackFor = Exception.class) public void resetPassword(Long id, String newPassword) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(R.CODE_NOT_FOUND, "user not found");
        u.setPasswordHash(passwordEncoder.encode(newPassword)); userMapper.updateById(u);
    }
    @Override @Transactional(rollbackFor = Exception.class) public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds != null && !roleIds.isEmpty())
            for (Long rid : roleIds) { SysUserRole ur = new SysUserRole(); ur.setUserId(userId); ur.setRoleId(rid); userRoleMapper.insert(ur); }
    }
    @Override @Transactional(rollbackFor = Exception.class) public void updatePassword(String username, String encryptedPassword) {
        SysUser u = loadByUsername(username);
        if (u == null) throw new BusinessException(R.CODE_NOT_FOUND, "user not found");
        u.setPasswordHash(encryptedPassword); userMapper.updateById(u);
    }
    @Override @Transactional(rollbackFor = Exception.class) public void recordLoginSuccess(String username, String ip) {
        SysUser u = loadByUsername(username); if (u == null) return;
        u.setLastLoginTime(LocalDateTime.now()); u.setLoginFailCount(0); u.setLockedUntil(null); userMapper.updateById(u);
    }
    @Override @Transactional(rollbackFor = Exception.class) public int recordLoginFailure(String username) {
        SysUser u = loadByUsername(username); if (u == null) return 0;
        int count = (u.getLoginFailCount() == null ? 0 : u.getLoginFailCount()) + 1;
        u.setLoginFailCount(count);
        if (count >= MAX_LOGIN_FAILURES) { u.setLockedUntil(LocalDateTime.now().plusMinutes(15)); u.setStatus(0); }
        userMapper.updateById(u); return count;
    }
}