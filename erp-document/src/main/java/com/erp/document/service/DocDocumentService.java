package com.erp.document.service;
import com.erp.document.dto.*;
public interface DocDocumentService {
    DocumentPageVO listPage(DocumentQuery q); DocumentVO getById(Long id);
    Long create(DocumentCreateRequest r); void finalize(Long id);
    void delete(Long id);
    /** 上传外部文件到 MinIO 并回填单证文件信息 */
    DocumentVO upload(Long id, String fileName, byte[] data, String contentType);
    /** 生成单证 PDF 并存入 MinIO（不落本地文件系统），同时把状态置为 final */
    DocumentVO generatePdf(Long id);
    /** 下载单证文件（从 MinIO 读取字节流） */
    DocumentDownload download(Long id);
}