package com.erp.data.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 上传记录共享部门。
 */
@Data
@TableName("dat_upload_dept_share")
public class DatUploadDeptShare {
    private Long id;
    private Long uploadId;
    private Long deptId;
    private LocalDateTime createdAt;
}
