package com.erp.report.dto;
import lombok.Builder; import lombok.Data;
/** 导出结果：返回文件名 + MinIO object key + 预签名下载地址 */
@Data @Builder
public class ReportExportVO {
    String fileName;
    String objectKey;
    String downloadUrl;
    long fileSize;
    int rowCount;
}
