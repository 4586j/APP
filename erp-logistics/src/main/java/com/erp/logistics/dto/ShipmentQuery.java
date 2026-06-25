package com.erp.logistics.dto;
import lombok.Data;
@Data
public class ShipmentQuery {
    String shipmentNo; String method; String status; String keyword; Integer page=1; Integer size=20;
}