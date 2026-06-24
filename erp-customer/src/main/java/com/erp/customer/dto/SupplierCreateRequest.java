package com.erp.customer.dto;
import jakarta.validation.constraints.NotBlank; import lombok.Data;
@Data
public class SupplierCreateRequest {
    @NotBlank String supplierCode; @NotBlank String supplierName;
    String province; String city; String contactPerson; String contactPhone;
    String contactEmail; String address; Integer rating;
    String paymentTerms; String bankName; String bankAccount;
    String taxId; String mainProducts; Integer cooperationYears;
}