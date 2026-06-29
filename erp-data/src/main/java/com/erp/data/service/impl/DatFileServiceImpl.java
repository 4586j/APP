package com.erp.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.data.dto.DatFileQuery;
import com.erp.data.dto.DatFileVO;
import com.erp.data.entity.DatFile;
import com.erp.data.entity.DatFileShare;
import com.erp.data.mapper.DatFileMapper;
import com.erp.data.mapper.DatFileShareMapper;
import com.erp.data.service.DatFileService;
import com.erp.security.user.LoginUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DatFileServiceImpl implements DatFileService {

    private final Path uploadRoot;
    private final DatFileMapper mapper;
    private final DatFileShareMapper shareMapper;
    private final JdbcTemplate jdbcTemplate;

    public DatFileServiceImpl(@Value("${app.upload.data-root:./uploads/data}") String dataRoot,
                              DatFileMapper mapper,
                              DatFileShareMapper shareMapper,
                              JdbcTemplate jdbcTemplate) {
        this.uploadRoot = Path.of(dataRoot).toAbsolutePath().normalize();
        this.mapper = mapper;
        this.shareMapper = shareMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==================== 辅助方法 ====================

    private Map<Long, String> loadUserNames(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        String inClause = userIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, real_name, username FROM sys_user WHERE id IN (" + inClause + ") AND deleted = 0");
        Map<Long, String> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            String realName = (String) row.get("real_name");
            String username = (String) row.get("username");
            result.put(id, realName != null && !realName.isBlank() ? realName : username);
        }
        return result;
    }

    private Map<Long, String> loadDeptNames(Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) return Collections.emptyMap();
        String inClause = deptIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, dept_name FROM sys_department WHERE id IN (" + inClause + ")");
        Map<Long, String> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(((Number) row.get("id")).longValue(), (String) row.get("dept_name"));
        }
        return result;
    }

    private List<Long> getDeptAndDescendantIds(Long deptId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id FROM sys_department WHERE dept_path LIKE CONCAT((SELECT dept_path FROM sys_department WHERE id = ?), '%') AND deleted = 0",
            deptId);
        return rows.stream().map(r -> ((Number) r.get("id")).longValue()).collect(Collectors.toList());
    }

    /** 检查用户是否有权限访问该文件/文件夹（含祖先共享继承）。 */
    @Override
    public boolean canAccess(DatFile file, LoginUser user) {
        if (file == null || user == null) return false;
        if (user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN")) return true;
        if (user.getId() != null && user.getId().equals(file.getCreatedBy())) return true;
        if (file.getDeptId() != null && user.getDepartmentId() != null) {
            List<Long> visibleDeptIds = getDeptAndDescendantIds(user.getDepartmentId());
            if (visibleDeptIds.contains(file.getDeptId())) return true;
        }
        if (user.getDepartmentId() != null && isSharedToMe(file, user.getDepartmentId())) return true;
        return false;
    }

    /** 检查用户是否有写入权限：本部门 OR 文件/祖先共享给本部门 OR 自己创建。 */
    @Override
    public boolean canWrite(DatFile file, LoginUser user) {
        if (file == null || user == null) return false;
        if (user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN")) return true;
        if (user.getId() != null && user.getId().equals(file.getCreatedBy())) return true;
        if (file.getDeptId() != null && file.getDeptId().equals(user.getDepartmentId())) return true;
        if (user.getDepartmentId() != null && isSharedToMe(file, user.getDepartmentId())) return true;
        return false;
    }

    @Override
    public boolean canCreate(Long targetDeptId, LoginUser user) {
        if (user == null) return false;
        if (user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN")) return true;
        return targetDeptId != null && targetDeptId.equals(user.getDepartmentId());
    }

    /** 文件本身或任一祖先被共享给本部门。 */
    private boolean isSharedToMe(DatFile file, Long myDeptId) {
        if (file == null || file.getId() == null) return false;
        if (shareMapper.selectDeptIdsByFileId(file.getId()).contains(myDeptId)) return true;
        List<Long> ancestorIds = parseAncestorIds(file.getPath());
        if (ancestorIds.isEmpty()) return false;
        return !mapper.selectSharedFileIdsIn(ancestorIds, myDeptId).isEmpty();
    }

    /** "/3/15/42/" → [3, 15, 42]。 */
    private List<Long> parseAncestorIds(String path) {
        if (path == null || path.isBlank()) return List.of();
        List<Long> ids = new ArrayList<>();
        for (String seg : path.split("/")) {
            if (seg.isBlank()) continue;
            try { ids.add(Long.valueOf(seg)); } catch (NumberFormatException ignore) {}
        }
        return ids;
    }

    private DatFileVO toVO(DatFile e) {
        if (e == null) return null;
        DatFileVO v = new DatFileVO();
        v.setId(e.getId());
        v.setParentId(e.getParentId());
        v.setIsDirectory(e.getIsDirectory());
        v.setName(e.getName());
        v.setDisplayName(e.getDisplayName());
        v.setExtension(e.getExtension());
        v.setMimeType(e.getMimeType());
        v.setFileSize(e.getFileSize());
        v.setFileType(e.getFileType());
        v.setDepartment(e.getDepartment());
        v.setDeptId(e.getDeptId());
        v.setRowCount(e.getRowCount());
        v.setParsed(e.getParsed());
        v.setRemark(e.getRemark());
        v.setCreatedBy(e.getCreatedBy());
        v.setCreatedAt(e.getCreatedAt());
        v.setUpdatedBy(e.getUpdatedBy());
        v.setUpdatedAt(e.getUpdatedAt());

        // 加载共享部门
        if (e.getId() != null) {
            List<Long> shareIds = shareMapper.selectDeptIdsByFileId(e.getId());
            v.setShareDeptIds(shareIds);
            if (!shareIds.isEmpty()) {
                Map<Long, String> nameMap = loadDeptNames(shareIds);
                v.setShareDeptNames(shareIds.stream().map(nameMap::get).filter(Objects::nonNull).collect(Collectors.toList()));
            }
        }
        return v;
    }

    /** 填充上传人姓名。 */
    private void fillCreatorNames(List<DatFileVO> list) {
        Set<Long> creatorIds = list.stream().map(DatFileVO::getCreatedBy)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        if (!creatorIds.isEmpty()) {
            Map<Long, String> nameMap = loadUserNames(creatorIds);
            list.forEach(v -> v.setCreatedByName(nameMap.get(v.getCreatedBy())));
        }
    }

    // ==================== 查询 ====================

    @Override
    public List<DatFileVO> listFiles(DatFileQuery q, LoginUser user) {
        List<DatFile> all;
        Long parentId = q.getParentId();

        if (parentId != null) {
            all = mapper.selectByParentId(parentId);
        } else if (q.getDeptId() != null) {
            all = mapper.selectRootFilesByDeptId(q.getDeptId());
        } else {
            all = mapper.selectRootFiles();
        }

        // 部门隔离过滤
        boolean isAdmin = user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN");
        List<Long> visibleDeptIds = Collections.emptyList();
        if (!isAdmin && user.getDepartmentId() != null) {
            visibleDeptIds = getDeptAndDescendantIds(user.getDepartmentId());
        }

        List<DatFile> filtered = new ArrayList<>();
        for (DatFile f : all) {
            if (isAdmin || canAccess(f, user)) {
                filtered.add(f);
            } else if (f.getIsDirectory() != null && f.getIsDirectory() == 1
                    && user.getDepartmentId() != null && f.getPath() != null
                    && mapper.selectSharedDescendantExists(f.getPath() + "/", user.getDepartmentId())) {
                // 文件夹本身不可见，但含被共享给本部门的后代 → 保留以便下钻
                filtered.add(f);
            }
        }

        // 关键词搜索
        if (q.getKeyword() != null && !q.getKeyword().isEmpty()) {
            String kw = q.getKeyword().toLowerCase();
            filtered = filtered.stream()
                .filter(f -> f.getName() != null && f.getName().toLowerCase().contains(kw))
                .collect(Collectors.toList());
        }

        // 文件类型过滤
        if (q.getFileType() != null && !q.getFileType().isEmpty()) {
            filtered = filtered.stream()
                .filter(f -> q.getFileType().equals(f.getFileType()))
                .collect(Collectors.toList());
        }

        List<DatFileVO> result = filtered.stream().map(this::toVO).collect(Collectors.toList());
        fillCreatorNames(result);
        return result;
    }

    @Override
    public List<DatFileVO> getBreadcrumb(Long fileId) {
        List<DatFileVO> path = new ArrayList<>();
        Long currentId = fileId;
        while (currentId != null) {
            DatFile f = mapper.selectById(currentId);
            if (f == null) break;
            path.add(0, toVO(f));
            currentId = f.getParentId();
        }
        return path;
    }

    // ==================== 创建 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFolder(Long parentId, String name, Long deptId, LoginUser user) {
        DatFile parent = parentId == null ? null : mapper.selectById(parentId);
        if (parentId != null && parent == null) throw new BusinessException(R.CODE_NOT_FOUND, "父目录不存在");
        if (parentId != null && !canWrite(parent, user)) throw new BusinessException(R.CODE_FORBIDDEN, "无权在此目录下创建");

        // 归属部门：显式传入优先（WebDAV 部门根），否则父目录部门，再否则用户本部门
        Long effectiveDeptId = deptId != null ? deptId
                : (parent != null ? parent.getDeptId() : user.getDepartmentId());

        DatFile f = new DatFile();
        f.setParentId(parentId);
        f.setIsDirectory(1);
        f.setName(name);
        f.setDisplayName(name);
        f.setDeptId(effectiveDeptId);
        f.setCreatedBy(user.getId());
        mapper.insert(f);
        String parentPath = parent == null ? "/" : parent.getPath();
        f.setPath(parentPath + f.getId() + "/");
        mapper.updateById(f);
        return f.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long uploadFile(MultipartFile file, Long parentId, String fileType,
                            Long deptId, List<Long> shareDeptIds, LoginUser user) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的文件");
        }

        // 检查父目录权限
        DatFile parent = parentId == null ? null : mapper.selectById(parentId);
        if (parentId != null && parent == null) throw new BusinessException(R.CODE_NOT_FOUND, "父目录不存在");
        if (parentId != null && !canWrite(parent, user)) throw new BusinessException(R.CODE_FORBIDDEN, "无权在此目录下上传");

        String originalName = StringUtils.cleanPath(
            file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename());
        String safeName = originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
        String suffix = "";
        int dot = safeName.lastIndexOf('.');
        if (dot >= 0) suffix = safeName.substring(dot);

        // 物理存储：按部门/日期分片
        Path deptDir = deptId != null ? uploadRoot.resolve(String.valueOf(deptId)) : uploadRoot.resolve("_common");
        Path dayDir = deptDir.resolve(LocalDate.now().toString().replace("-", ""));
        String storedName = UUID.randomUUID() + suffix;
        Path target = dayDir.resolve(storedName).normalize();

        try {
            Files.createDirectories(dayDir);
            file.transferTo(target);
        } catch (IOException ex) {
            throw new IllegalStateException("文件保存失败", ex);
        }

        DatFile f = new DatFile();
        f.setParentId(parentId);
        f.setIsDirectory(0);
        f.setName(safeName);
        f.setDisplayName(originalName);
        f.setExtension(suffix.isEmpty() ? null : suffix);
        f.setMimeType(file.getContentType());
        f.setFileSize(file.getSize());
        f.setStoragePath(target.toString());
        f.setFileType(fileType);
        f.setDeptId(deptId);
        f.setCreatedBy(user.getId());
        mapper.insert(f);

        // 维护 path
        String parentPath = parent == null ? "/" : parent.getPath();
        f.setPath(parentPath + f.getId() + "/");
        mapper.updateById(f);

        // 保存共享部门
        saveShareDepts(f.getId(), shareDeptIds);

        return f.getId();
    }

    private void saveShareDepts(Long fileId, List<Long> deptIds) {
        if (deptIds != null && !deptIds.isEmpty()) {
            for (Long sid : deptIds) {
                if (sid == null) continue;
                DatFileShare s = new DatFileShare();
                s.setFileId(fileId);
                s.setDeptId(sid);
                shareMapper.insert(s);
            }
        }
    }

    // ==================== 修改 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rename(Long id, String newName, LoginUser user) {
        DatFile f = mapper.selectById(id);
        if (f == null || f.getDeleted() == 1) throw new BusinessException(R.CODE_NOT_FOUND, "文件不存在");
        if (!canWrite(f, user)) throw new BusinessException(R.CODE_FORBIDDEN, "无权重命名");

        f.setDisplayName(newName);
        f.setUpdatedBy(user.getId());
        mapper.updateById(f);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(Long id, Long targetParentId, LoginUser user) {
        DatFile f = mapper.selectById(id);
        if (f == null || f.getDeleted() == 1) throw new BusinessException(R.CODE_NOT_FOUND, "文件不存在");
        if (!canWrite(f, user)) throw new BusinessException(R.CODE_FORBIDDEN, "无权移动");

        DatFile target = null;
        if (targetParentId != null) {
            target = mapper.selectById(targetParentId);
            if (target == null || target.getIsDirectory() != 1) {
                throw new BusinessException(R.CODE_PARAM_INVALID, "目标目录不存在或不是文件夹");
            }
            if (!canWrite(target, user)) throw new BusinessException(R.CODE_FORBIDDEN, "无权写入目标目录");
        }

        String oldPrefix = f.getPath();
        String newPrefix = (target == null ? "/" : target.getPath()) + f.getId() + "/";
        f.setPath(newPrefix);
        f.setParentId(targetParentId);
        f.setUpdatedBy(user.getId());
        mapper.updateById(f);
        if (f.getIsDirectory() != null && f.getIsDirectory() == 1) {
            mapper.updatePathPrefix(oldPrefix, newPrefix);
        }
    }

    // ==================== 删除 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, LoginUser user) {
        DatFile f = mapper.selectById(id);
        if (f == null || f.getDeleted() == 1) throw new BusinessException(R.CODE_NOT_FOUND, "文件不存在");
        if (!canWrite(f, user)) throw new BusinessException(R.CODE_FORBIDDEN, "无权删除");

        // 文件夹：沿 path 前缀递归软删自身 + 所有后代
        if (f.getIsDirectory() != null && f.getIsDirectory() == 1 && f.getPath() != null) {
            mapper.softDeleteByPathPrefix(f.getPath());
        }
        f.setDeleted(1);
        f.setUpdatedBy(user.getId());
        mapper.updateById(f);
    }

    // ==================== 下载 ====================

    @Override
    public void download(Long id, HttpServletResponse response) {
        DatFile f = mapper.selectById(id);
        if (f == null || f.getDeleted() == 1) {
            throw new BusinessException(R.CODE_NOT_FOUND, "file not found");
        }
        if (f.getIsDirectory() == 1) {
            throw new BusinessException(R.CODE_PARAM_INVALID, "文件夹不能下载");
        }
        if (f.getStoragePath() == null || f.getStoragePath().isEmpty()) {
            throw new BusinessException(R.CODE_PARAM_INVALID, "file path not set");
        }
        Path file = Path.of(f.getStoragePath());
        if (!Files.exists(file)) {
            throw new BusinessException(R.CODE_NOT_FOUND, "file not found on disk");
        }
        try {
            String encodedName = URLEncoder.encode(f.getDisplayName() != null ? f.getDisplayName() : f.getName(),
                StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename*=utf-8''" + encodedName);
            response.setContentLengthLong(Files.size(file));
            try (InputStream in = Files.newInputStream(file)) {
                in.transferTo(response.getOutputStream());
            }
            response.flushBuffer();
        } catch (IOException ex) {
            throw new IllegalStateException("file download failed", ex);
        }
    }

    // ==================== 覆写内容 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void writeContent(Long fileId, java.io.InputStream in, LoginUser user) {
        DatFile f = mapper.selectById(fileId);
        if (f == null || f.getDeleted() == 1) {
            throw new BusinessException(R.CODE_NOT_FOUND, "文件不存在");
        }
        if (f.getIsDirectory() == 1) {
            throw new BusinessException(R.CODE_PARAM_INVALID, "不能写入文件夹");
        }
        if (!canWrite(f, user)) {
            throw new BusinessException(R.CODE_FORBIDDEN, "无权修改该文件");
        }
        java.nio.file.Path target = java.nio.file.Path.of(f.getStoragePath());
        try {
            java.nio.file.Files.createDirectories(target.getParent());
            long size;
            try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(target,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                size = in.transferTo(out);
            }
            f.setFileSize(size);
            f.setUpdatedBy(user.getId());
            mapper.updateById(f);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("文件保存失败", ex);
        }
    }
}
