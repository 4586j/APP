package com.erp.document.dto;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder
public class DocumentVO {
    Long id; String docNo; String docType; Long orderId; String orderNo; Long shipmentId;
    String title; String status; String templateCode; String filePath; String fileName; Long fileSize;
    Long generatedBy; LocalDateTime generatedAt; String remark; LocalDateTime createdAt;
}