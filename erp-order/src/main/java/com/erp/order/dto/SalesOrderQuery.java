package com.erp.order.dto;
import lombok.Data; import java.time.LocalDate;
@Data
public class SalesOrderQuery {
    String keyword; Long customerId; String status;
    LocalDate dateFrom; LocalDate dateTo;
    Integer page = 1; Integer size = 10;
}