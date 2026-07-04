package com.erp.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.data.entity.DatFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

@Mapper
public interface DatFileMapper extends BaseMapper<DatFile> {

    /**
     * 查询某目录下的直接子项。
     */
    @Select("SELECT * FROM dat_file WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY is_directory DESC, name ASC, id DESC")
    List<DatFile> selectByParentId(Long parentId);

    /**
     * 查询根目录文件/文件夹（parent_id IS NULL）。
     */
    @Select("SELECT * FROM dat_file WHERE parent_id IS NULL AND deleted = 0 ORDER BY is_directory DESC, name ASC, id DESC")
    List<DatFile> selectRootFiles();

    /**
     * 查询某部门的所有文件。
     */
    @Select("SELECT * FROM dat_file WHERE dept_id = #{deptId} AND deleted = 0 ORDER BY created_at DESC")
    List<DatFile> selectByDeptId(Long deptId);

    /**
     * 查询某部门的根目录文件/文件夹（parent_id IS NULL）。
     */
    @Select("SELECT * FROM dat_file WHERE dept_id = #{deptId} AND parent_id IS NULL AND deleted = 0 ORDER BY is_directory DESC, name ASC, id DESC")
    List<DatFile> selectRootFilesByDeptId(Long deptId);

    /**
     * 查询用户上传的文件。
     */
    @Select("SELECT * FROM dat_file WHERE created_by = #{userId} AND deleted = 0 ORDER BY created_at DESC")
    List<DatFile> selectByUserId(Long userId);

    /**
     * 查询 path 前缀下是否存在被共享给指定部门的后代文件/文件夹。
     *
     * @param pathPrefix 形如 "/3/15/"（含末尾斜杠）
     * @param deptId     被共享部门
     * @return true=存在
     */
    @Select("SELECT EXISTS(SELECT 1 FROM dat_file f " +
            "JOIN dat_file_share s ON s.file_id = f.id " +
            "WHERE f.path LIKE CONCAT(#{pathPrefix}, '%') AND s.dept_id = #{deptId} AND f.deleted = 0)")
    boolean selectSharedDescendantExists(@Param("pathPrefix") String pathPrefix, @Param("deptId") Long deptId);

    /**
     * 查询一批 file id 中，被共享给指定部门的 id（用于祖先共享判定）。
     *
     * @param ancestorIds 祖先文件 id 集合
     * @param deptId      被共享部门
     * @return 被共享的文件 id 列表（非空即表示有祖先共享权限）
     */
    @Select("<script>SELECT s.file_id FROM dat_file_share s WHERE s.dept_id = #{deptId} " +
            "AND s.file_id IN " +
            "<foreach collection='ancestorIds' item='aid' open='(' separator=',' close=')'>#{aid}</foreach></script>")
    List<Long> selectSharedFileIdsIn(@Param("ancestorIds") Collection<Long> ancestorIds, @Param("deptId") Long deptId);

    /**
     * 移动文件夹时，把子树 path 的旧前缀替换为新前缀。
     *
     * @param oldPrefix 旧前缀，如 "/3/15/"
     * @param newPrefix 新前缀，如 "/7/15/"
     * @return 受影响行数
     */
    @Update("UPDATE dat_file SET path = CONCAT(#{newPrefix}, SUBSTRING(path, LENGTH(#{oldPrefix}) + 1)) " +
            "WHERE path LIKE CONCAT(#{oldPrefix}, '%')")
    int updatePathPrefix(@Param("oldPrefix") String oldPrefix, @Param("newPrefix") String newPrefix);

    /**
     * 软删除某路径前缀下的所有记录（自身 + 全部后代）。
     * pathPrefix 形如 "/3/15/"，{@code LIKE '/3/15/%'} 同时匹配自身 "/3/15/" 与后代。
     *
     * @param pathPrefix 文件夹自身的 path（含末尾斜杠）
     * @return 受影响行数
     */
    @Update("UPDATE dat_file SET deleted = 1 WHERE path LIKE CONCAT(#{pathPrefix}, '%') AND deleted = 0")
    int softDeleteByPathPrefix(@Param("pathPrefix") String pathPrefix);
}
