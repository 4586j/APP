package com.erp.order.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("ord_status_history")
public class OrdStatusHistory {
    @TableId(type = IdType.AUTO) private Long id;
    private String orderType; private Long orderId;
    private String fromStatus; private String toStatus;
    private Long operator; private String remark;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
}