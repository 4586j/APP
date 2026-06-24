package com.erp.order.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Data; import java.math.BigDecimal; import java.time.LocalDate; import java.util.List;
@Data
public class PurchaseOrderCreateRequest {
    @NotNull Long supplierId; Long relatedSalesOrderId; @NotNull LocalDate orderDate;
    LocalDate expectedDelivery; String currency = "CNY"; String paymentTerms;
    BigDecimal exchangeRate; String remarks;
    @NotNull List<PurchaseItemRequest> items;
    @Data
    public static class PurchaseItemRequest {
        @NotNull Long productId; String productCode; String productName; String specification;
        @NotNull BigDecimal quantity; String unit; @NotNull BigDecimal unitPrice;
        Long relatedSalesItemId;
    }
}