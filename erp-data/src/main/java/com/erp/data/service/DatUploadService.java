package com.erp.data.service;
import com.erp.data.dto.DataUploadPageVO; import com.erp.data.dto.DataUploadQuery; import com.erp.data.dto.DataUploadVO;
public interface DatUploadService {
    DataUploadPageVO listPage(DataUploadQuery q);
    DataUploadVO getById(Long id);
    Long upload(String fileName, String fileType, Long fileSize, String department);
    void delete(Long id);
}