package com.erp.user.dto;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class PermissionTreeNode {
    private Long id;
    private Long parentId;
    private String name;
    private String code;
    private String type;
    private String httpMethod;
    private String icon;
    private String path;
    private String component;
    private Integer sortOrder;
    private Integer status;
    private List<PermissionTreeNode> children = new ArrayList<>();
}
