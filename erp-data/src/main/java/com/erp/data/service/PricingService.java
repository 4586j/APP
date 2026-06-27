package com.erp.data.service;
import com.erp.data.dto.ImportTaskVO;
import com.erp.data.dto.PricingPageVO; import com.erp.data.dto.PricingQuery;
import com.erp.data.dto.PricingVO; import com.erp.data.dto.PricingCreateRequest;
import org.springframework.web.multipart.MultipartFile;

public interface PricingService {
    PricingPageVO listPage(PricingQuery q);
    PricingVO getById(Long id);
    Long create(PricingCreateRequest r);
    void update(Long id, PricingCreateRequest r);
    void delete(Long id);

    /**
     * 提交异步导入任务,上传文件后返回 taskId。
     */
    ImportTaskVO submitImportTask(MultipartFile file);

    /**
     * 查询导入任务进度/结果。
     */
    ImportTaskVO getImportTask(String taskId);
}
