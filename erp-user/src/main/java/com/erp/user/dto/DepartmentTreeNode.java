package com.erp.user.dto;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class DepartmentTreeNode {
    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private String deptPath;
    private Integer sortOrder;
    private List<DepartmentTreeNode> children = new ArrayList<>();
}