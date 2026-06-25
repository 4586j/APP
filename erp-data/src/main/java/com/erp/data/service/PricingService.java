package com.erp.data.service;
import com.erp.common.dto.BatchImportResult;
import com.erp.data.dto.PricingPageVO; import com.erp.data.dto.PricingQuery;
import com.erp.data.dto.PricingVO; import com.erp.data.dto.PricingCreateRequest;
import java.io.InputStream;

public interface PricingService {
    PricingPageVO listPage(PricingQuery q);
    PricingVO getById(Long id);
    Long create(PricingCreateRequest r);
    void update(Long id, PricingCreateRequest r);
    void delete(Long id);
    BatchImportResult batchImport(InputStream inputStream);
}