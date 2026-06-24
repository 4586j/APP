package com.erp.finance.dto;
import lombok.Builder; import lombok.Data; import java.util.List;
@Data @Builder
public class FundApprovalPageVO {
    List<FundApprovalVO> records; long total; long size; long current;
}