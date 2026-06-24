package com.erp.customer.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("cust_supplier")
public class CustSupplier {
    @TableId(type = IdType.AUTO) private Long id;
    private String supplierCode; private String supplierName;
    private String province; private String city;
    private String contactPerson; private String contactPhone; private String contactEmail;
    private String address; private Integer rating;
    private String paymentTerms; private String bankName; private String bankAccount;
    private String taxId; private String mainProducts; private Integer cooperationYears;
    private Integer status;
    private Long createdBy; private Long updatedBy;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}