package com.erp.order.dto;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull;
import lombok.Data; import java.math.BigDecimal; import java.time.LocalDate; import java.util.List;
@Data
public class SalesOrderCreateRequest {
    @NotNull Long customerId; String customerOrderNo; @NotNull LocalDate orderDate;
    String currency = "USD"; String tradeTerms = "FOB"; String paymentTerms;
    String portLoading; String portDestination; LocalDate expectedDelivery;
    BigDecimal exchangeRate; String remarks;
    @NotNull List<SalesOrderItemRequest> items;
    @Data
    public static class SalesOrderItemRequest {
        @NotNull Long productId; String productCode; String productName;
        String hsCode; String specification;
        @NotNull BigDecimal quantity; String unit; @NotNull BigDecimal unitPrice;
    }
}