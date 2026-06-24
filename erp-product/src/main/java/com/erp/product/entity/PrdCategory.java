package com.erp.product.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("prd_category")
public class PrdCategory {
    @TableId(type = IdType.AUTO) private Long id;
    private Long parentId; private String catName; private String catCode;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}