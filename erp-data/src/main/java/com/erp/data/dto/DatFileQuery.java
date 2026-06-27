package com.erp.data.dto;

import lombok.Data;

import java.util.List;

/**
 * 网盘文件查询参数。
 */
@Data
public class DatFileQuery {
    private Long parentId;
    private String keyword;
    private String fileType;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
