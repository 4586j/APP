package com.erp.finance.service;
import com.erp.finance.dto.*;
public interface FinReceivableService {
    ReceivablePageVO listPage(ReceivableQuery q);
    ReceivableVO getById(Long id);
    Long create(ReceivableCreateRequest r);
    void update(Long id, ReceivableUpdateRequest r);
    void delete(Long id);

    /** 确认收款：累加已收金额并刷新状态(pending/partial/received) */
    void confirmPayment(Long id, ConfirmPaymentRequest r);
    /** 批量全额确认收款，返回成功条数 */
    int batchConfirm(java.util.List<Long> ids);
    /** 账龄统计（按到期日距今天数分桶，金额取未结清余额） */
    AgingReportVO aging();
}