package com.erp.user.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门选择器选项节点（扁平/树形）。
 */
@Data
public class DepartmentOption {
    private Long id;
    private Long parentId;
    private String name;
    private String code;
    private Integer sortOrder;
    private Integer status;
    private List<DepartmentOption> children = new ArrayList<>();
}
