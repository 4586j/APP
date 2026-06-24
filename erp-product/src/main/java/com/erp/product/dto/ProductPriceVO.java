package com.erp.product.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate;
@Data @Builder
public class ProductPriceVO {
    Long id; Long productId; String priceType; String currencyCode;
    BigDecimal price; LocalDate validFrom; LocalDate validTo;
}