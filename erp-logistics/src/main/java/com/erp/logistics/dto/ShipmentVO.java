package com.erp.logistics.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime;
@Data @Builder
public class ShipmentVO {
    Long id; String shipmentNo; Long orderId; String orderNo; Long customerId; String customerName;
    String method; String status; String carrier; String vesselFlight; String containerNo;
    String sealNo; String blNo; LocalDate etd; LocalDate eta; String portLoading; String portDischarge;
    BigDecimal grossWeight; BigDecimal netWeight; BigDecimal volume; Integer packageCount;
    String shippingMarks; String remark; LocalDateTime createdAt;
}