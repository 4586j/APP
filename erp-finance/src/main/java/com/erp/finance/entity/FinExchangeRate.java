package com.erp.finance.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime;
@Data @TableName("fin_exchange_rate")
public class FinExchangeRate {
    @TableId(type = IdType.AUTO) private Long id;
    private String currencyFrom; private String currencyTo;
    private BigDecimal rate; private LocalDate rateDate; private String source;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @Version private Integer version;
    @TableLogic private Integer deleted;
}