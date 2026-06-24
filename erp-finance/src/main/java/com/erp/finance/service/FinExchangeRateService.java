package com.erp.finance.service;
import com.erp.finance.dto.*;
public interface FinExchangeRateService {
    ExchangeRatePageVO listPage(ExchangeRateQuery q);
    Long create(ExchangeRateCreateRequest r);
    void delete(Long id);
}