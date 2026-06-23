package com.erp.common.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 所有业务实体的基类（B0.3 骨架）。
 *
 * <p>统一审计字段 + 逻辑删除 + 乐观锁，由 MyBatis-Plus 自动填充。
 * <p>具体的 MetaObjectHandler 在 B1.2 阶段实现（数据库连通后）。
 */
@Data
public abstract class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID（雪花算法，由 MP 自动生成） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 创建时间（INSERT 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（INSERT + UPDATE 自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 创建人 ID（INSERT 自动填充，取自 SecurityContext） */
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    /** 更新人 ID（INSERT + UPDATE 自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    /** 逻辑删除标志：0=正常 1=已删除 */
    @TableLogic
    @TableField(value = "deleted")
    private Integer deleted;

    /** 乐观锁版本号（每次更新自增 1） */
    @Version
    private Integer version;
}
