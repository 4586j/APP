package com.erp.finance.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime;
@Data @TableName("fin_receivable")
public class FinReceivable {
    @TableId(type=IdType.AUTO) Long id;
    String receiptNo; String sourceType; Long sourceId; Long customerId; String customerName;
    BigDecimal totalAmount; BigDecimal receivedAmount;
    LocalDate dueDate; String currency="CNY"; String status="pending"; String remark;
    Integer deleted=0;
    LocalDateTime createdAt; LocalDateTime updatedAt;
}