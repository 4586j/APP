package com.erp.finance.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime;
@Data @Builder
public class ExchangeRateVO {
    Long id; String currencyFrom; String currencyTo;
    BigDecimal rate; LocalDate rateDate; String source; LocalDateTime createdAt;
}