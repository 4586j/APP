package com.erp.order.dto;
import lombok.Data; import java.math.BigDecimal; import java.time.LocalDate;
@Data
public class SalesOrderUpdateRequest {
    String customerOrderNo; LocalDate expectedDelivery;
    String portLoading; String portDestination; String paymentTerms;
    String remarks; BigDecimal exchangeRate;
}