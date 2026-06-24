package com.erp.customer.dto;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder
public class SupplierVO {
    Long id; String supplierCode; String supplierName;
    String province; String city; String contactPerson; String contactPhone;
    String contactEmail; String address; Integer rating;
    String paymentTerms; String bankName; String bankAccount;
    String taxId; String mainProducts; Integer cooperationYears;
    Integer status; LocalDateTime createdAt;
}