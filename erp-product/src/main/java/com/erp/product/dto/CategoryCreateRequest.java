package com.erp.product.dto;
import jakarta.validation.constraints.NotBlank; import lombok.Data;
@Data
public class CategoryCreateRequest {
    private Long parentId; @NotBlank private String catName; @NotBlank private String catCode; private Integer sortOrder;
}