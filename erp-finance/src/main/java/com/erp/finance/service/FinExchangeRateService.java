package com.erp.finance.service;
import com.erp.finance.dto.*;
public interface FinExchangeRateService {
    ExchangeRatePageVO listPage(ExchangeRateQuery q);
    Long create(ExchangeRateCreateRequest r);
    void delete(Long id);

    /** 当前汇率：取 from→to 在「今天及之前」生效的最新一条 */
    ExchangeRateVO getCurrent(String from, String to);
}