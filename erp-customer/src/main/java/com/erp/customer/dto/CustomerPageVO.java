package com.erp.customer.dto;
import lombok.Builder; import lombok.Data; import java.util.List;
@Data @Builder
public class CustomerPageVO {
    List<CustomerVO> records; long total; long size; long current;
}