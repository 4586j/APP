package com.erp.logistics.dto;
import jakarta.validation.constraints.NotNull; import lombok.Data;
import java.time.LocalDateTime;
@Data
public class TrackingCreateRequest {
    @NotNull Long shipmentId; @NotNull LocalDateTime trackingDate;
    String location; String eventCode; String description; String operator;
}