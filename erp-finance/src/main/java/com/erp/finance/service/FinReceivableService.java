package com.erp.finance.service;
import com.erp.finance.dto.*;
public interface FinReceivableService {
    ReceivablePageVO listPage(ReceivableQuery q);
    ReceivableVO getById(Long id);
    Long create(ReceivableCreateRequest r);
    void delete(Long id);
}