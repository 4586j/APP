package com.erp.product.dto;
import jakarta.validation.constraints.NotBlank; import lombok.Data;
import java.math.BigDecimal;
@Data
public class ProductCreateRequest {
    @NotBlank String productCode; @NotBlank String productName; String productNameEn;
    Long categoryId; Long hsCodeId; String unit; String specification;
    String originCountry; String brand;
    BigDecimal purchasePrice; BigDecimal salesPrice; BigDecimal costPrice;
    BigDecimal weightKg; BigDecimal volumeCbm; Integer moq;
}