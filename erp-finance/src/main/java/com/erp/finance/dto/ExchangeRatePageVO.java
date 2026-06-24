package com.erp.finance.dto;
import lombok.Builder; import lombok.Data; import java.util.List;
@Data @Builder
public class ExchangeRatePageVO {
    List<ExchangeRateVO> records; long total; long size; long current;
}