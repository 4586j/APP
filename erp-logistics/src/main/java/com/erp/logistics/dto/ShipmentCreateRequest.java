package com.erp.logistics.dto;
import jakarta.validation.constraints.NotBlank; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate;
@Data
public class ShipmentCreateRequest {
    @NotBlank String method; Long orderId; String orderNo;
    Long customerId; String customerName; String carrier; String vesselFlight;
    String containerNo; String sealNo; String blNo;
    LocalDate etd; LocalDate eta; String portLoading; String portDischarge;
    BigDecimal grossWeight; BigDecimal netWeight; BigDecimal volume; Integer packageCount;
    String shippingMarks; String remark;
}