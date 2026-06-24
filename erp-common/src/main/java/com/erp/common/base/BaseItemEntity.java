package com.erp.common.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 明细表实体基类（如订单明细 / 采购明细 / 单证明细等）。
 *
 * <p>与 {@link BaseEntity} 的区别：
 * <ul>
 *   <li>不支持逻辑删除 — 明细与主表生命周期绑定，主表逻辑删除后明细物理保留以保证审计</li>
 *   <li>不支持乐观锁 — 明细以"主表 id + 行号"为业务键，更新通过整表 diff 处理</li>
 *   <li>不持有创建人 / 更新人 — 这些信息由主表统一维护，明细行人字段=主表创建人</li>
 *   <li>只保留 id / createdAt / updatedAt 三个最小审计字段</li>
 * </ul>
 *
 * <p>典型用法：
 * <pre>{@code
 * @Data
 * @EqualsAndHashCode(callSuper = true)
 * @TableName("sales_order_item")
 * public class SalesOrderItem extends BaseItemEntity {
 *     private Long orderId;           // 主表 FK
 *     private Integer lineNo;         // 行号
 *     private Long productId;
 *     private BigDecimal quantity;
 *     private BigDecimal unitPrice;
 * }
 * }</pre>
 */
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class BaseItemEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID（雪花算法） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 创建时间（INSERT 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（INSERT + UPDATE 自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
