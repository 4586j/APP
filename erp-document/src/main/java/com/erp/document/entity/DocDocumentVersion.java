package com.erp.document.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("doc_document_version")
public class DocDocumentVersion {
    @TableId(type=IdType.AUTO) Long id; Long documentId; Integer versionNo;
    String filePath; String fileName; Long fileSize;
    Long createdBy; String remark; LocalDateTime createdAt;
}