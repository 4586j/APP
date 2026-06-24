package com.erp.product.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @Builder
public class ProductVO {
    Long id; String productCode; String productName; String productNameEn;
    Long categoryId; Long hsCodeId; String hsCode; String unit; String specification;
    String originCountry; String brand;
    BigDecimal purchasePrice; BigDecimal salesPrice; BigDecimal costPrice;
    BigDecimal weightKg; BigDecimal volumeCbm; Integer moq; Integer status;
    LocalDateTime createdAt;
}