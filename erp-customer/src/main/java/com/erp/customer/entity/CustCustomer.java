package com.erp.customer.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @TableName("cust_customer")
public class CustCustomer {
    @TableId(type = IdType.AUTO) private Long id;
    private String customerCode; private String customerName; private String customerNameEn;
    private String customerType; private String country;
    private String contactPerson; private String contactEmail; private String contactPhone;
    private String contactFax; private String website; private String address;
    private BigDecimal creditLimit; private String paymentTerms;
    private String taxId; private String swiftCode; private String bankName; private String bankAccount;
    private Integer status;
    private Long createdBy; private Long updatedBy;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}