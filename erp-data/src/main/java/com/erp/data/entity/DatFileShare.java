package com.erp.data.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 网盘文件/文件夹共享部门。
 */
@Data
@TableName("dat_file_share")
public class DatFileShare {
    private Long id;
    private Long fileId;
    private Long deptId;
    private LocalDateTime createdAt;
}
