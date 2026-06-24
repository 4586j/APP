package com.erp.user.dto;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String avatarUrl;
    private Long departmentId;
    private String departmentName;
    private Long superiorId;
    private Integer status;
    private Integer pwdResetRequired;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime lockedUntil;
    private LocalDateTime createdAt;
    private List<String> roleCodes;
    private List<String> permCodes;
}