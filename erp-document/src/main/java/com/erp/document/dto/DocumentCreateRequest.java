package com.erp.document.dto;
import jakarta.validation.constraints.NotBlank; import lombok.Data;
@Data
public class DocumentCreateRequest {
    @NotBlank String docType; Long orderId; String orderNo; Long shipmentId;
    @NotBlank String title; String templateCode; String remark;
}