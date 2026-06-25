package com.erp.finance.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
/** 批量确认收/付款 */
@Data
public class BatchConfirmRequest {
    @NotNull List<Long> ids;
}
