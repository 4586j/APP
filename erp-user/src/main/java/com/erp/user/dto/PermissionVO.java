package com.erp.user.dto;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PermissionVO {
    private Long id;
    private String name;
    private String code;
    private String type;
    private Long parentId;
    private Integer sortOrder;
    private String path;
    private String icon;
    private Integer status;
    private LocalDateTime createdAt;
}