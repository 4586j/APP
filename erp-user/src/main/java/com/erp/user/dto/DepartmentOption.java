package com.erp.user.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门选择器选项节点（扁平/树形）。
 *
 * <p>id / parentId 为雪花 ID，超过 JS 安全整数范围，序列化为字符串避免前端精度丢失。
 */
@Data
public class DepartmentOption {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    private String name;
    private String code;
    private Integer sortOrder;
    private Integer status;
    private List<DepartmentOption> children = new ArrayList<>();
}
