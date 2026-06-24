package com.erp.order.dto;
import jakarta.validation.constraints.NotBlank; import lombok.Data;
@Data
public class StatusChangeRequest {
    @NotBlank String toStatus; String remark;
}