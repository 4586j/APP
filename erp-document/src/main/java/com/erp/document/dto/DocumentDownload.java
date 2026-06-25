package com.erp.document.dto;
import lombok.Builder; import lombok.Data;
@Data @Builder
public class DocumentDownload {
    String fileName; String contentType; byte[] data;
}
