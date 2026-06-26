package com.erp.user.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.dto.BatchImportResult;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.alibaba.excel.EasyExcel;
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
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "erp.user.persistence", havingValue = "mysql", matchIfMissing = true)
public class UserServiceImpl implements UserService {
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysDepartmentMapper departmentMapper;
    private final SysDepartmentPermissionMapper deptPermMapper;
    private final PasswordEncoder passwordEncoder;
    private static final int MAX_LOGIN_FAILURES = 5;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    @Override public SysUser loadByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }
    @Override public List<String> getRolesByUserId(Long userId) { return roleMapper.selectRolesByUserId(userId); }
    @Override public List<String> getPermissionsByUserId(Long userId) {
        // 角色权限
        Set<String> perms = new HashSet<>(permissionMapper.selectPermissionsByUserId(userId));
        // 部门权限（用户所在部门赋予的权限）
        SysUser u = userMapper.selectById(userId);
        if (u != null && u.getDepartmentId() != null) {
            perms.addAll(permissionMapper.selectPermissionsByDeptId(u.getDepartmentId()));
        }
        return new ArrayList<>(perms);
    }

    @Override public Page<?> pageUsers(UserQuery query) {
        Page<SysUser> page = new Page<>(query.getPage(), Math.min(query.getSize(), 100));
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        if (query.getUsername() != null) w.like(SysUser::getUsername, query.getUsername());
        if (query.getRealName() != null) w.like(SysUser::getRealName, query.getRealName());
        if (query.getStatus() != null) w.eq(SysUser::getStatus, query.getStatus());

        // 按角色过滤
        if (query.getRoleId() != null) {
            List<Long> userIds = userRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, query.getRoleId()))
                .stream().map(SysUserRole::getUserId).distinct().collect(java.util.stream.Collectors.toList());
            if (userIds.isEmpty()) {
                // 无匹配用户，返回空页
                w.eq(SysUser::getId, -1L);
            } else {
                w.in(SysUser::getId, userIds);
            }
        }
        // 排除某角色的用户
        if (query.getExcludeRoleId() != null) {
            List<Long> userIds = userRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, query.getExcludeRoleId()))
                .stream().map(SysUserRole::getUserId).distinct().collect(java.util.stream.Collectors.toList());
            if (!userIds.isEmpty()) {
                w.notIn(SysUser::getId, userIds);
            }
        }

        w.orderByDesc(SysUser::getCreatedAt);
        return userMapper.selectPage(page, w);
    }

    @Override public UserVO getUserById(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BusinessException(R.CODE_NOT_FOUND, "user not found");

        // 查询部门名称
        String deptName = null;
        if (u.getDepartmentId() != null) {
            SysDepartment dept = departmentMapper.selectById(u.getDepartmentId());
            if (dept != null) deptName = dept.getName();
        }

        // 查询角色ID列表
        List<Long> rids = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id))
            .stream().map(SysUserRole::getRoleId).collect(Collectors.toList());

        return UserVO.builder().id(u.getId()).username(u.getUsername()).realName(u.getRealName())
            .email(u.getEmail()).phone(u.getPhone()).departmentId(u.getDepartmentId())
            .departmentName(deptName)
            .status(u.getStatus()).lastLoginTime(u.getLastLoginTime()).createdAt(u.getCreatedAt())
            .roleIds(rids)
            .build();
    }

    @Override @Transactional(rollbackFor = Exception.class) public Long createUser(UserCreateRequest req) {
        if (loadByUsername(req.getUsername()) != null) throw new BusinessException(R.CODE_PARAM_INVALID, "username exists");
        SysUser u = new SysUser(); u.setUsername(req.getUsername());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setRealName(req.getRealName()); u.setEmail(req.getEmail()); u.setPhone(req.getPhone());
        u.setDepartmentId(req.getDepartmentId()); u.setStatus(1);
        userMapper.insert(u);
        // 绑定角色
        if (req.getRoleIds() != null && !req.getRoleIds().isEmpty()) {
            for (Long rid : req.getRoleIds()) {
                SysUserRole ur = new SysUserRole(); ur.setUserId(u.getId()); ur.setRoleId(rid); userRoleMapper.insert(ur);
            }
        }
        return u.getId();
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

    /**
     * 内部创建用户（无事务），供批量导入调用。
     * 调用方需自行处理事务边界。
     */
    private Long doCreateUser(UserCreateRequest req) {
        SysUser u = new SysUser();
        u.setUsername(req.getUsername());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setRealName(req.getRealName());
        u.setEmail(req.getEmail());
        u.setPhone(req.getPhone());
        u.setDepartmentId(req.getDepartmentId());
        u.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        userMapper.insert(u);
        // 绑定角色
        if (req.getRoleIds() != null && !req.getRoleIds().isEmpty()) {
            for (Long rid : req.getRoleIds()) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(u.getId());
                ur.setRoleId(rid);
                userRoleMapper.insert(ur);
            }
        }
        return u.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchImportResult batchCreateUsers(List<UserCreateRequest> list) {
        BatchImportResult result = new BatchImportResult();
        if (list == null || list.isEmpty()) return result;

        // 批量预加载已有用户名，避免 N+1 查询
        Set<String> existingUsernames = userMapper.selectList(null).stream()
                .map(SysUser::getUsername)
                .collect(Collectors.toSet());

        for (int i = 0; i < list.size(); i++) {
            UserCreateRequest req = list.get(i);
            if (existingUsernames.contains(req.getUsername())) {
                BatchImportResult.FailItem item = new BatchImportResult.FailItem();
                item.setIndex(i + 1);
                item.setName(req.getUsername());
                item.setReason("用户名已存在");
                result.getFailList().add(item);
                continue;
            }
            try {
                doCreateUser(req);
                result.setSuccessCount(result.getSuccessCount() + 1);
                existingUsernames.add(req.getUsername());
            } catch (Exception e) {
                BatchImportResult.FailItem item = new BatchImportResult.FailItem();
                item.setIndex(i + 1);
                item.setName(req.getUsername());
                item.setReason("系统异常: " + e.getMessage());
                result.getFailList().add(item);
            }
        }
        return result;
    }

    @Override
    public BatchImportResult importUsersFromExcel(InputStream inputStream) {
        List<UserImportExcelDTO> excelList = EasyExcel.read(inputStream)
                .head(UserImportExcelDTO.class)
                .sheet()
                .doReadSync();

        BatchImportResult result = new BatchImportResult();
        if (excelList == null || excelList.isEmpty()) return result;

        // 预加载部门和角色映射
        Map<String, Long> deptNameMap = departmentMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDepartment::getName, SysDepartment::getId, (a, b) -> a));
        Map<String, Long> roleCodeMap = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getRoleCode, SysRole::getId, (a, b) -> a));
        // 预加载已有用户名，避免循环内重复查库
        Set<String> existingUsernames = userMapper.selectList(null).stream()
                .map(SysUser::getUsername)
                .collect(Collectors.toSet());

        for (int i = 0; i < excelList.size(); i++) {
            UserImportExcelDTO dto = excelList.get(i);
            StringBuilder reason = new StringBuilder();

            // 校验
            if (!StringUtils.hasText(dto.getUsername())) {
                reason.append("用户名为空; ");
            }
            if (!StringUtils.hasText(dto.getRealName())) {
                reason.append("姓名为空; ");
            }
            if (StringUtils.hasText(dto.getEmail()) && !EMAIL_PATTERN.matcher(dto.getEmail()).matches()) {
                reason.append("邮箱格式错误; ");
            }
            if (StringUtils.hasText(dto.getPhone()) && !PHONE_PATTERN.matcher(dto.getPhone()).matches()) {
                reason.append("手机号格式错误; ");
            }
            if (StringUtils.hasText(dto.getDepartmentName()) && !deptNameMap.containsKey(dto.getDepartmentName())) {
                reason.append("部门不存在; ");
            }
            if (StringUtils.hasText(dto.getRoleCode()) && !roleCodeMap.containsKey(dto.getRoleCode())) {
                reason.append("角色不存在; ");
            }

            if (reason.length() > 0) {
                BatchImportResult.FailItem item = new BatchImportResult.FailItem();
                item.setIndex(i + 2); // Excel 行号从 2 开始（含表头）
                item.setName(dto.getUsername());
                item.setReason(reason.toString().trim());
                result.getFailList().add(item);
                continue;
            }

            // 检查用户名唯一性（内存判断）
            String trimmedUsername = dto.getUsername().trim();
            if (existingUsernames.contains(trimmedUsername)) {
                BatchImportResult.FailItem item = new BatchImportResult.FailItem();
                item.setIndex(i + 2);
                item.setName(dto.getUsername());
                item.setReason("用户名已存在");
                result.getFailList().add(item);
                continue;
            }

            try {
                UserCreateRequest req = new UserCreateRequest();
                req.setUsername(trimmedUsername);
                req.setRealName(dto.getRealName().trim());
                req.setPassword(trimmedUsername); // 默认密码 = 用户名
                req.setEmail(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null);
                req.setPhone(StringUtils.hasText(dto.getPhone()) ? dto.getPhone().trim() : null);
                req.setDepartmentId(deptNameMap.get(dto.getDepartmentName()));
                req.setStatus(1);
                // Excel 导入默认绑定角色
                if (StringUtils.hasText(dto.getRoleCode())) {
                    Long roleId = roleCodeMap.get(dto.getRoleCode());
                    if (roleId != null) {
                        req.setRoleIds(Collections.singletonList(roleId));
                    }
                }

                doCreateUser(req);

                result.setSuccessCount(result.getSuccessCount() + 1);
                existingUsernames.add(trimmedUsername);
            } catch (Exception e) {
                BatchImportResult.FailItem item = new BatchImportResult.FailItem();
                item.setIndex(i + 2);
                item.setName(dto.getUsername());
                item.setReason("创建失败: " + e.getMessage());
                result.getFailList().add(item);
            }
        }
        return result;
    }
}