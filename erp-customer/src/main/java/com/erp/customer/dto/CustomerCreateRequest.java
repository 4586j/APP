package com.erp.customer.dto;
import jakarta.validation.constraints.NotBlank; import lombok.Data;
import java.math.BigDecimal;
@Data
public class CustomerCreateRequest {
    @NotBlank String customerCode; @NotBlank String customerName; String customerNameEn;
    String customerType; String country; String contactPerson; String contactEmail;
    String contactPhone; String contactFax; String website; String address;
    BigDecimal creditLimit; String paymentTerms;
    String taxId; String swiftCode; String bankName; String bankAccount;
}