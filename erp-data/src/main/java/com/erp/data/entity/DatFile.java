package com.erp.data.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企业网盘文件/文件夹（自引用树结构）。
 */
@Data
@TableName("dat_file")
public class DatFile {
    private Long id;
    private Long parentId;
    private Integer isDirectory;
    private String name;
    private String displayName;
    private String extension;
    private String mimeType;
    private Long fileSize;
    private String storagePath;
    private String fileType;
    private String department;
    private Long deptId;
    private Integer rowCount;
    private Integer parsed;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
