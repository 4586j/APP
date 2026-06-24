package com.erp.customer.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @Builder
public class CustomerVO {
    Long id; String customerCode; String customerName; String customerNameEn;
    String customerType; String country;
    String contactPerson; String contactEmail; String contactPhone;
    String contactFax; String website; String address;
    BigDecimal creditLimit; String paymentTerms;
    String taxId; String swiftCode; String bankName; String bankAccount;
    Integer status; LocalDateTime createdAt;
}