package com.erp.finance.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @TableName("fin_settlement")
public class FinSettlement {
    @TableId(type=IdType.AUTO) Long id;
    String settlementNo; String direction;
    Long receivableId; Long payableId; String relatedType; Long relatedId;
    BigDecimal amount; String paymentMethod="bank_transfer"; String bankAccount;
    String description; Long settledBy; LocalDateTime settledAt;
    LocalDateTime createdAt; LocalDateTime updatedAt;
}