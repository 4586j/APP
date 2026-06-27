package com.erp.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.data.dto.DataUploadPageVO;
import com.erp.data.dto.DataUploadQuery;
import com.erp.data.dto.DataUploadVO;
import com.erp.data.entity.DatUpload;
import com.erp.data.entity.DatUploadDeptShare;
import com.erp.data.mapper.DatUploadDeptShareMapper;
import com.erp.data.mapper.DatUploadMapper;
import com.erp.data.service.DatUploadService;
import com.erp.security.user.LoginUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DatUploadServiceImpl implements DatUploadService {
    private final Path uploadRoot;
    private final DatUploadMapper mapper;
    private final DatUploadDeptShareMapper shareMapper;
    private final JdbcTemplate jdbcTemplate;

    public DatUploadServiceImpl(@Value("${app.upload.data-root:./uploads/data}") String dataRoot,
                                DatUploadMapper mapper,
                                DatUploadDeptShareMapper shareMapper,
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

    /** 查询某部门及其所有下级部门 ID（含自身），基于 dept_path 前缀匹配。 */
    private List<Long> getDeptAndDescendantIds(Long deptId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id FROM sys_department WHERE dept_path LIKE CONCAT((SELECT dept_path FROM sys_department WHERE id = ?), '%') AND deleted = 0",
            deptId);
        return rows.stream().map(r -> ((Number) r.get("id")).longValue()).collect(Collectors.toList());
    }

    /** 批量加载共享部门 ID → 部门名映射。 */
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

    // ==================== 查询 ====================

    @Override
    public DataUploadPageVO listPage(DataUploadQuery q, LoginUser user) {
        LambdaQueryWrapper<DatUpload> w = new LambdaQueryWrapper<>();
        w.eq(DatUpload::getDeleted, 0);

        // 部门层级隔离 + 共享部门可见
        boolean isAdmin = user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN");
        if (!isAdmin && user.getId() != null) {
            // 自己上传的 OR 部门层级可见 OR 共享部门可见
            List<Long> visibleDeptIds = new ArrayList<>();
            if (user.getDepartmentId() != null) {
                visibleDeptIds.addAll(getDeptAndDescendantIds(user.getDepartmentId()));
            }
            if (!visibleDeptIds.isEmpty()) {
                String deptIdStr = visibleDeptIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                w.and(i -> i.eq(DatUpload::getCreatedBy, user.getId())
                    .or().in(DatUpload::getDeptId, visibleDeptIds)
                    .or().apply("EXISTS (SELECT 1 FROM dat_upload_dept_share s WHERE s.upload_id = id AND s.dept_id IN (" + deptIdStr + "))"));
            } else {
                w.eq(DatUpload::getCreatedBy, user.getId());
            }
        }

        if (q.getKeyword() != null && !q.getKeyword().isEmpty()) {
            w.like(DatUpload::getFileName, q.getKeyword());
        }
        if (q.getFileType() != null && !q.getFileType().isEmpty()) {
            w.eq(DatUpload::getFileType, q.getFileType());
        }
        w.orderByDesc(DatUpload::getCreatedAt);
        Page<DatUpload> p = mapper.selectPage(new Page<>(q.getPageNum(), Math.min(q.getPageSize(), 100)), w);
        List<DataUploadVO> voList = p.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        // 批量填充上传人姓名
        Set<Long> creatorIds = voList.stream().map(DataUploadVO::getCreatedBy)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> nameMap = loadUserNames(creatorIds);
        voList.forEach(v -> v.setCreatedByName(nameMap.get(v.getCreatedBy())));
        return new DataUploadPageVO(p.getTotal(), q.getPageNum(), q.getPageSize(), voList);
    }

    @Override
    public DataUploadVO getById(Long id) {
        DataUploadVO vo = toVO(mapper.selectById(id));
        if (vo != null && vo.getCreatedBy() != null) {
            Map<Long, String> nameMap = loadUserNames(Collections.singleton(vo.getCreatedBy()));
            vo.setCreatedByName(nameMap.get(vo.getCreatedBy()));
        }
        return vo;
    }

    // ==================== 上传 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long upload(String fileName, String fileType, Long fileSize, String department,
                        Long userId, Long deptId, List<Long> shareDeptIds) {
        DatUpload e = new DatUpload();
        e.setFileName(fileName);
        e.setFileType(fileType);
        e.setOriginalName(fileName);
        e.setFileSize(fileSize);
        e.setDepartment(department);
        e.setDeptId(deptId);
        e.setRowCount(0);
        e.setParsed(false);
        e.setUploadType("manual");
        if (userId != null && userId > 0) e.setCreatedBy(userId);
        mapper.insert(e);
        saveShareDepts(e.getId(), shareDeptIds);
        return e.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long uploadFile(MultipartFile file, String fileType, String department,
                            Long userId, Long deptId, List<Long> shareDeptIds) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的文件");
        }
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename());
        String safeName = originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
        String suffix = "";
        int dot = safeName.lastIndexOf('.');
        if (dot >= 0) {
            suffix = safeName.substring(dot);
        }
        Path dayDir = uploadRoot.resolve(LocalDate.now().toString());
        String storedName = UUID.randomUUID() + suffix;
        Path target = dayDir.resolve(storedName).normalize();
        try {
            Files.createDirectories(dayDir);
            file.transferTo(target);
        } catch (IOException ex) {
            throw new IllegalStateException("文件保存失败", ex);
        }

        DatUpload e = new DatUpload();
        e.setFileName(safeName);
        e.setOriginalName(originalName);
        e.setFileType(fileType);
        e.setFileSize(file.getSize());
        e.setFilePath(target.toString());
        e.setDepartment(department);
        e.setDeptId(deptId);
        e.setRowCount(0);
        e.setParsed(false);
        e.setUploadType("file");
        if (userId != null && userId > 0) e.setCreatedBy(userId);
        mapper.insert(e);
        saveShareDepts(e.getId(), shareDeptIds);
        return e.getId();
    }

    /** 保存共享部门记录（覆盖式）。 */
    private void saveShareDepts(Long uploadId, List<Long> shareDeptIds) {
        if (shareDeptIds != null && !shareDeptIds.isEmpty()) {
            for (Long sid : shareDeptIds) {
                if (sid == null) continue;
                DatUploadDeptShare s = new DatUploadDeptShare();
                s.setUploadId(uploadId);
                s.setDeptId(sid);
                shareMapper.insert(s);
            }
        }
    }

    // ==================== 删除 / 下载 ====================

    @Override
    public void delete(Long id) {
        int updated = mapper.update(null, new LambdaUpdateWrapper<DatUpload>()
            .eq(DatUpload::getId, id)
            .eq(DatUpload::getDeleted, 0)
            .set(DatUpload::getDeleted, 1));
        if (updated == 0) {
            throw new BusinessException(R.CODE_NOT_FOUND, "upload not found");
        }
    }

    @Override
    public void download(Long id, HttpServletResponse response) {
        DatUpload e = mapper.selectById(id);
        if (e == null || e.getDeleted() == 1) {
            throw new BusinessException(R.CODE_NOT_FOUND, "upload not found");
        }
        if (e.getFilePath() == null || e.getFilePath().isEmpty()) {
            throw new BusinessException(R.CODE_PARAM_INVALID, "file path not set");
        }
        Path file = Path.of(e.getFilePath());
        if (!Files.exists(file)) {
            throw new BusinessException(R.CODE_NOT_FOUND, "file not found on disk");
        }
        try {
            String encodedName = URLEncoder.encode(e.getFileName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
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

    // ==================== VO 转换 ====================

    private DataUploadVO toVO(DatUpload e) {
        if (e == null) return null;
        DataUploadVO v = new DataUploadVO();
        v.setId(e.getId());
        v.setFileName(e.getFileName());
        v.setFileType(e.getFileType());
        v.setOriginalName(e.getOriginalName());
        v.setFileSize(e.getFileSize());
        v.setFilePath(e.getFilePath());
        v.setDepartment(e.getDepartment());
        v.setDeptId(e.getDeptId());
        v.setRowCount(e.getRowCount());
        v.setParsed(e.getParsed());
        v.setRemark(e.getRemark());
        v.setCreatedBy(e.getCreatedBy());
        v.setCreatedAt(e.getCreatedAt());

        // 加载共享部门信息
        if (e.getId() != null) {
            List<Long> shareIds = shareMapper.selectDeptIdsByUploadId(e.getId());
            v.setShareDeptIds(shareIds);
            if (!shareIds.isEmpty()) {
                Map<Long, String> nameMap = loadDeptNames(shareIds);
                v.setShareDeptNames(shareIds.stream().map(nameMap::get).filter(Objects::nonNull).collect(Collectors.toList()));
            }
        }
        return v;
    }
}
