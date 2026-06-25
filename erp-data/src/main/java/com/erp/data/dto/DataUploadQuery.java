package com.erp.data.dto;
import com.erp.common.base.BaseQuery;
import lombok.Data; import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true)
public class DataUploadQuery extends BaseQuery {
    private String keyword; private String fileType;
}