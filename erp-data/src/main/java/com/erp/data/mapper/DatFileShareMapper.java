package com.erp.data.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.data.entity.DatFileShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DatFileShareMapper extends BaseMapper<DatFileShare> {

    @Select("SELECT dept_id FROM dat_file_share WHERE file_id = #{fileId}")
    List<Long> selectDeptIdsByFileId(Long fileId);

    default int deleteByFileId(Long fileId) {
        return delete(new LambdaQueryWrapper<DatFileShare>().eq(DatFileShare::getFileId, fileId));
    }
}
