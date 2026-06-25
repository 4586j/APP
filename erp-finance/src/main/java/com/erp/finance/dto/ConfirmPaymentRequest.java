package com.erp.finance.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
/** 确认收/付款：本次金额 */
@Data
public class ConfirmPaymentRequest {
    @NotNull BigDecimal amount;
    String remark;
}
