package com.erp.common.base;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用分页查询基类（B0.3 骨架）。
 */
@Data
public abstract class BaseQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String orderBy;
    private String orderDir = "DESC";
}
