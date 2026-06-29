package com.erp.data.service;

import com.erp.data.dto.DatFileQuery;
import com.erp.data.dto.DatFileVO;
import com.erp.data.entity.DatFile;
import com.erp.security.user.LoginUser;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DatFileService {

    /**
     * 列出目录内容（含部门隔离）。
     */
    List<DatFileVO> listFiles(DatFileQuery q, LoginUser user);

    /**
     * 获取面包屑路径。
     */
    List<DatFileVO> getBreadcrumb(Long fileId);

    /**
     * 新建文件夹。
     *
     * @param deptId 目标部门 id（WebDAV 在部门根下建文件夹时由 URL 解析得出，
     *               决定新文件夹归属部门）。传 null 时回退：父目录有则用父目录部门，
     *               父目录为空（部门根）则用当前用户所在部门。
     */
    Long createFolder(Long parentId, String name, Long deptId, LoginUser user);

    /**
     * 上传文件到指定目录。
     */
    Long uploadFile(MultipartFile file, Long parentId, String fileType,
                    Long deptId, List<Long> shareDeptIds, LoginUser user);

    /**
     * 重命名。
     */
    void rename(Long id, String newName, LoginUser user);

    /**
     * 移动到其他目录。
     */
    void move(Long id, Long targetParentId, LoginUser user);

    /**
     * 删除（逻辑删除）。
     */
    void delete(Long id, LoginUser user);

    /**
     * 下载文件。
     */
    void download(Long id, HttpServletResponse response);

    /**
     * 检查用户是否可在指定部门目录下新建（仅本部门）。
     */
    boolean canCreate(Long targetDeptId, LoginUser user);

    /**
     * 覆盖已存在文件的内容（WebDAV PUT 保存），不改动 deptId。
     */
    void writeContent(Long fileId, java.io.InputStream in, LoginUser user);

    /**
     * 检查写权限（WebDAV LOCK/PUT 校验用）。
     */
    boolean canWrite(DatFile file, LoginUser user);

    /**
     * 检查读权限（WebDAV PROPFIND/GET 校验用，含祖先共享继承）。
     */
    boolean canAccess(DatFile file, LoginUser user);
}
