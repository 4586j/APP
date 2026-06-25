package com.erp.finance.service;
import com.erp.finance.dto.*;
public interface FinPayableService {
    PayablePageVO listPage(PayableQuery q);
    PayableVO getById(Long id);
    Long create(PayableCreateRequest r);
    void update(Long id, PayableUpdateRequest r);
    void delete(Long id);

    /** 确认付款：累加已付金额并刷新状态(pending/partial/paid) */
    void confirmPayment(Long id, ConfirmPaymentRequest r);
    /** 批量全额确认付款，返回成功条数 */
    int batchConfirm(java.util.List<Long> ids);
    /** 账龄统计（按到期日距今天数分桶，金额取未结清余额） */
    AgingReportVO aging();
}