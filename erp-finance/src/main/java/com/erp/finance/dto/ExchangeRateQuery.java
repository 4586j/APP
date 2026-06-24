package com.erp.finance.dto;
import lombok.Data; import java.time.LocalDate;
@Data
public class ExchangeRateQuery {
    LocalDate date; String currency; Integer page=1; Integer size=20;
}