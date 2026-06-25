package com.erp.document.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("doc_document")
public class DocDocument {
    @TableId(type=IdType.AUTO) Long id; String docNo; String docType;
    Long orderId; String orderNo; Long shipmentId; String title;
    String status="draft"; String templateCode;
    String filePath; String fileName; Long fileSize;
    String minioObjectKey; String contentType;
    Long generatedBy; LocalDateTime generatedAt; String remark;
    Integer deleted=0; LocalDateTime createdAt; LocalDateTime updatedAt;
}