package com.erp.data.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.data.entity.DatUploadDeptShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DatUploadDeptShareMapper extends BaseMapper<DatUploadDeptShare> {

    @Select("SELECT dept_id FROM dat_upload_dept_share WHERE upload_id = #{uploadId}")
    List<Long> selectDeptIdsByUploadId(Long uploadId);

    default int deleteByUploadId(Long uploadId) {
        return delete(new LambdaQueryWrapper<DatUploadDeptShare>().eq(DatUploadDeptShare::getUploadId, uploadId));
    }
}
