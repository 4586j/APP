package com.erp.data.service;
import com.erp.data.dto.PricingPageVO; import com.erp.data.dto.PricingQuery;
import com.erp.data.dto.PricingVO; import com.erp.data.dto.PricingCreateRequest;
public interface PricingService {
    PricingPageVO listPage(PricingQuery q);
    PricingVO getById(Long id);
    Long create(PricingCreateRequest r);
    void update(Long id, PricingCreateRequest r);
    void delete(Long id);
}