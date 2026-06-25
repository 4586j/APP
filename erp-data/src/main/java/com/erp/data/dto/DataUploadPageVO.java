package com.erp.data.dto;
import com.erp.common.model.PageResult;
import java.util.List;
public class DataUploadPageVO extends PageResult<DataUploadVO> {
    public DataUploadPageVO(long total, long pageNum, long pageSize, List<DataUploadVO> records) {
        super(total, pageNum, pageSize, (total + pageSize - 1) / pageSize, records);
    }
}