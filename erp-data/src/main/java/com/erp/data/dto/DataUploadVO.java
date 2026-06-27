package com.erp.data.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DataUploadVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String fileName;
    private String fileType;
    private String originalName;
    private Long fileSize;
    private String filePath;
    private String uploadType;
    private String department;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;
    private List<Long> shareDeptIds;
    private List<String> shareDeptNames;
    private Integer rowCount;
    private Boolean parsed;
    private String remark;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
}
