package com.erp.finance.dto;
import jakarta.validation.constraints.NotBlank; import lombok.Data;
@Data
public class ApproveRequest {
    @NotBlank String action; // approved / rejected
    String comment;
}