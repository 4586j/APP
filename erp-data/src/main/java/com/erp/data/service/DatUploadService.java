package com.erp.data.service;

import com.erp.data.dto.DataUploadPageVO;
import com.erp.data.dto.DataUploadQuery;
import com.erp.data.dto.DataUploadVO;
import com.erp.security.user.LoginUser;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DatUploadService {
    DataUploadPageVO listPage(DataUploadQuery q, LoginUser user);
    DataUploadVO getById(Long id);

    /**
     * 上传文件元数据（不保存实体文件）。
     */
    Long upload(String fileName, String fileType, Long fileSize, String department,
                Long userId, Long deptId, List<Long> shareDeptIds);

    /**
     * 上传文件并保存到磁盘。
     */
    Long uploadFile(MultipartFile file, String fileType, String department,
                    Long userId, Long deptId, List<Long> shareDeptIds);
    void delete(Long id);
    void download(Long id, HttpServletResponse response);
}
