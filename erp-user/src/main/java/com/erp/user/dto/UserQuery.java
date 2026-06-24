package com.erp.user.dto;
import lombok.Data;

@Data
public class UserQuery {
    private String username;
    private String realName;
    private Long departmentId;
    private Integer status;
    private Integer page = 1;
    private Integer size = 10;
}
