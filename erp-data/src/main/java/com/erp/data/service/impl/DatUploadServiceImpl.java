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
import com.erp.data.mapper.DatUploadMapper;
import com.erp.data.service.DatUploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
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
    /** 上传文件存储根目录，默认相对工作目录 ./uploads/data，可由配置 app.upload.data-root 覆盖。 */
    private final Path uploadRoot;

    private final DatUploadMapper mapper;
    private final JdbcTemplate jdbcTemplate;

    public DatUploadServiceImpl(@Value("${app.upload.data-root:./uploads/data}") String dataRoot,
                                DatUploadMapper mapper,
                                JdbcTemplate jdbcTemplate) {
        this.uploadRoot = Path.of(dataRoot).toAbsolutePath().normalize();
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 批量查询用户 ID → 用户名映射（只查 sys_user，避免与 erp-user 模块循环依赖）。 */
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

    @Override
    public DataUploadPageVO listPage(DataUploadQuery q) {
        LambdaQueryWrapper<DatUpload> w = new LambdaQueryWrapper<>();
        w.eq(DatUpload::getDeleted, 0);
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

    @Override
    public Long upload(String fileName, String fileType, Long fileSize, String department, Long userId) {
        DatUpload e = new DatUpload();
        e.setFileName(fileName);
        e.setFileType(fileType);
        e.setOriginalName(fileName);
        e.setFileSize(fileSize);
        e.setDepartment(department);
        e.setRowCount(0);
        e.setParsed(false);
        e.setUploadType("manual");
        if (userId != null && userId > 0) e.setCreatedBy(userId);
        mapper.insert(e);
        return e.getId();
    }

    @Override
    public Long uploadFile(MultipartFile file, String fileType, String department, Long userId) {
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
        e.setRowCount(0);
        e.setParsed(false);
        e.setUploadType("file");
        if (userId != null && userId > 0) e.setCreatedBy(userId);
        mapper.insert(e);
        return e.getId();
    }

    /**
     * 逻辑删除：只标记 dat_upload.deleted=1，不删除磁盘文件，便于审计/回滚。
     */
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
        v.setRowCount(e.getRowCount());
        v.setParsed(e.getParsed());
        v.setRemark(e.getRemark());
        v.setCreatedBy(e.getCreatedBy());
        v.setCreatedAt(e.getCreatedAt());
        return v;
    }
}
