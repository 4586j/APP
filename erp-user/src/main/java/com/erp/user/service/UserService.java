package com.erp.user.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.dto.BatchImportResult;
import com.erp.user.dto.*;
import com.erp.user.entity.SysUser;
import java.util.List;

public interface UserService {
    SysUser loadByUsername(String username);
    List<String> getRolesByUserId(Long userId);
    List<String> getPermissionsByUserId(Long userId);
    Page<UserVO> pageUsers(UserQuery query);
    UserVO getUserById(Long id);
    Long createUser(UserCreateRequest req);
    void updateUser(Long id, UserUpdateRequest req);
    void deleteUser(Long id);
    void lockUser(Long id);
    void unlockUser(Long id);
    void resetPassword(Long id, String newPassword);
    void assignRoles(Long userId, List<Long> roleIds);
    void updatePassword(String username, String encryptedPassword);
    void recordLoginSuccess(String username, String ip);
    int recordLoginFailure(String username);

    /**
     * 批量创建用户。
     */
    BatchImportResult batchCreateUsers(List<UserCreateRequest> list);

    /**
     * 从 Excel 导入用户。
     */
    BatchImportResult importUsersFromExcel(java.io.InputStream inputStream);
}