package com.erp.order.dto;
import lombok.Builder; import lombok.Data; import java.util.List;
@Data @Builder
public class PurchaseOrderPageVO {
    List<PurchaseOrderVO> records; long total; long size; long current;
}