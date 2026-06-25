package com.erp.finance.dto;
import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate;
/** 应付更新：可改供应商名/到期日/金额/备注 */
@Data
public class PayableUpdateRequest {
    String supplierName; BigDecimal totalAmount; LocalDate dueDate; String remark;
}
