package com.erp.logistics.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime;
@Data @TableName("log_shipment")
public class LogShipment {
    @TableId(type=IdType.AUTO) Long id; String shipmentNo;
    Long orderId; String orderNo; Long customerId; String customerName;
    String method; String status="booked"; String carrier; String vesselFlight;
    String containerNo; String sealNo; String blNo;
    LocalDate etd; LocalDate eta; String portLoading; String portDischarge;
    BigDecimal grossWeight; BigDecimal netWeight; BigDecimal volume;
    Integer packageCount; String shippingMarks; String remark;
    Integer deleted=0; LocalDateTime createdAt; LocalDateTime updatedAt;
}