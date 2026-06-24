package com.erp.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录后返回的用户信息（B1.4 Phase 1），对齐 API_DESIGN §2.1。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    private Long id;
    private String username;
    private String realName;
    private String department;
    private String departmentName;
    private List<String> roles;
    private List<String> permissions;
}
