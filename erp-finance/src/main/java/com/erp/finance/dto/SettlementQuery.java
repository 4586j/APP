package com.erp.finance.dto;
import lombok.Data;
@Data
public class SettlementQuery {
    String direction; Long receivableId; Long payableId; Integer page=1; Integer size=20;
}