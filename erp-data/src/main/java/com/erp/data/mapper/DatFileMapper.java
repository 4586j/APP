package com.erp.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.data.entity.DatFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DatFileMapper extends BaseMapper<DatFile> {

    /**
     * 查询某目录下的直接子项。
     */
    @Select("SELECT * FROM dat_file WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY is_directory DESC, name ASC")
    List<DatFile> selectByParentId(Long parentId);

    /**
     * 查询根目录文件/文件夹（parent_id IS NULL）。
     */
    @Select("SELECT * FROM dat_file WHERE parent_id IS NULL AND deleted = 0 ORDER BY is_directory DESC, name ASC")
    List<DatFile> selectRootFiles();

    /**
     * 查询某部门的所有文件。
     */
    @Select("SELECT * FROM dat_file WHERE dept_id = #{deptId} AND deleted = 0 ORDER BY created_at DESC")
    List<DatFile> selectByDeptId(Long deptId);

    /**
     * 查询某部门的根目录文件/文件夹（parent_id IS NULL）。
     */
    @Select("SELECT * FROM dat_file WHERE dept_id = #{deptId} AND parent_id IS NULL AND deleted = 0 ORDER BY is_directory DESC, name ASC")
    List<DatFile> selectRootFilesByDeptId(Long deptId);

    /**
     * 查询用户上传的文件。
     */
    @Select("SELECT * FROM dat_file WHERE created_by = #{userId} AND deleted = 0 ORDER BY created_at DESC")
    List<DatFile> selectByUserId(Long userId);
}
