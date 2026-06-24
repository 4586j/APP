package com.erp.finance.dto;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull;
import lombok.Data; import java.math.BigDecimal; import java.time.LocalDate;
@Data
public class ExchangeRateCreateRequest {
    @NotBlank String currencyFrom; @NotBlank String currencyTo;
    @NotNull BigDecimal rate; @NotNull LocalDate rateDate; String source;
}