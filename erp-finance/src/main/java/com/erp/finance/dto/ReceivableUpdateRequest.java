package com.erp.finance.dto;
import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate;
/** 应收更新：可改客户名/到期日/金额/备注 */
@Data
public class ReceivableUpdateRequest {
    String customerName; BigDecimal totalAmount; LocalDate dueDate; String remark;
}
