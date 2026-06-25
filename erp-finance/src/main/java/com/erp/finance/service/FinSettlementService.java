package com.erp.finance.service;
import com.erp.finance.dto.*;
public interface FinSettlementService {
    SettlementPageVO listPage(SettlementQuery q);
    SettlementVO getById(Long id);
    Long create(SettlementCreateRequest r);
    void delete(Long id);
}