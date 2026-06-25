package com.erp.finance.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime;
@Data @TableName("fin_payable")
public class FinPayable {
    @TableId(type=IdType.AUTO) Long id;
    String payNo; String sourceType; Long sourceId; Long supplierId; String supplierName;
    BigDecimal totalAmount; BigDecimal paidAmount;
    LocalDate dueDate; String currency="CNY"; String status="pending"; String remark;
    Integer deleted=0;
    @Version Integer version;
    LocalDateTime createdAt; LocalDateTime updatedAt;
}