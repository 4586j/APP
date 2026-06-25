package com.erp.data.dto;
import lombok.Data; import java.time.LocalDateTime;
@Data public class DataUploadVO {
    private Long id; private String fileName; private String fileType;
    private String originalName; private Long fileSize; private String filePath;
    private String uploadType; private String department; private Integer rowCount;
    private Boolean parsed; private String remark;
    private Long createdBy; private LocalDateTime createdAt;
}