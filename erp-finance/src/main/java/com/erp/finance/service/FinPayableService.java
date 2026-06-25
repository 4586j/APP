package com.erp.finance.service;
import com.erp.finance.dto.*;
public interface FinPayableService {
    PayablePageVO listPage(PayableQuery q);
    PayableVO getById(Long id);
    Long create(PayableCreateRequest r);
    void delete(Long id);
}