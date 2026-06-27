package com.erp.data.service;

import com.erp.data.dto.DataUploadPageVO;
import com.erp.data.dto.DataUploadQuery;
import com.erp.data.dto.DataUploadVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface DatUploadService {
    DataUploadPageVO listPage(DataUploadQuery q, com.erp.security.user.LoginUser user);
    DataUploadVO getById(Long id);
    Long upload(String fileName, String fileType, Long fileSize, String department, Long userId);

    /**
     * 上传文件并保存到磁盘，手动指定创建人 userId（覆盖 MetaObjectHandler 的自动填充）。
     */
    Long uploadFile(MultipartFile file, String fileType, String department, Long userId);
    void delete(Long id);
    void download(Long id, HttpServletResponse response);
}
