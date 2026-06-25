package com.erp.data.dto;
import com.erp.common.model.PageResult;
import java.util.List;
public class PricingPageVO extends PageResult<PricingVO> {
    public PricingPageVO(long total, long pageNum, long pageSize, List<PricingVO> records) {
        super(total, pageNum, pageSize, (total + pageSize - 1) / pageSize, records);
    }
}