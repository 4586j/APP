package com.erp.product.dto;
import lombok.Builder; import lombok.Data; import java.util.List;
@Data @Builder
public class ProductPageVO {
    List<ProductVO> records; long total; long size; long current;
}