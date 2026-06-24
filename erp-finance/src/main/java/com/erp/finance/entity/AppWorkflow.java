package com.erp.finance.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("app_workflow")
public class AppWorkflow {
    @TableId(type=IdType.AUTO) Long id;
    String workflowCode;
    String workflowName;
    String targetType;
    String description;
    Integer status=1;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}