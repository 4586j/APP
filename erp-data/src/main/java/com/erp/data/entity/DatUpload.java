package com.erp.data.entity;
import com.erp.common.base.BaseEntity;
import lombok.Data; import lombok.EqualsAndHashCode;
@Data @EqualsAndHashCode(callSuper=true)
public class DatUpload extends BaseEntity {
    private String fileName; private String fileType; private String originalName;
    private Long fileSize; private String filePath; private String uploadType;
    private String department; private Integer rowCount;
    private Boolean parsed; private String remark;
}