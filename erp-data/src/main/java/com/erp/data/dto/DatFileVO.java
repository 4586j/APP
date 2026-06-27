package com.erp.data.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 网盘文件/文件夹视图模型。
 */
@Data
public class DatFileVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    private Integer isDirectory;
    private String name;
    private String displayName;
    private String extension;
    private String mimeType;
    private Long fileSize;
    private String fileType;
    private String department;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;
    private List<Long> shareDeptIds;
    private List<String> shareDeptNames;
    private Integer rowCount;
    private Integer parsed;
    private String remark;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
