package com.erp.finance.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("app_approval_history")
public class AppApprovalHistory {
    @TableId(type=IdType.AUTO) Long id;
    Long requestId;
    Long nodeId;
    Long approver;
    String action;
    String comment;
    LocalDateTime createdAt;
}