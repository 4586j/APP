package com.erp.data.service;

import com.erp.data.dto.DataUploadPageVO;
import com.erp.data.dto.DataUploadQuery;
import com.erp.data.dto.DataUploadVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface DatUploadService {
    DataUploadPageVO listPage(DataUploadQuery q);
    DataUploadVO getById(Long id);
    Long upload(String fileName, String fileType, Long fileSize, String department);
    Long uploadFile(MultipartFile file, String fileType, String department);
    void delete(Long id);
    void download(Long id, HttpServletResponse response);
}
