package com.erp.product.dto;
import lombok.Builder; import lombok.Data;
import java.util.List; import java.time.LocalDateTime;
@Data @Builder
public class PrdCategoryVO {
    Long id; Long parentId; String catName; String catCode;
    Integer sortOrder; LocalDateTime createdAt;
    List<PrdCategoryVO> children;
}