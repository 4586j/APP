package com.erp.finance.dto;
import lombok.Builder; import lombok.Data; import java.util.List;
@Data @Builder
public class PayablePageVO { List<PayableVO> records; long total; long size; long current; }