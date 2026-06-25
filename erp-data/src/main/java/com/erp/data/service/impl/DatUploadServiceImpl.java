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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DatUploadServiceImpl implements DatUploadService {
    private static final Path UPLOAD_ROOT = Path.of("/code/demo2/uploads/data");

    private final DatUploadMapper mapper;

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
        Page<DatUpload> p = mapper.selectPage(new Page<>(q.getPageNum(), q.getPageSize()), w);
        return new DataUploadPageVO(p.getTotal(), q.getPageNum(), q.getPageSize(),
            p.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    @Override
    public DataUploadVO getById(Long id) {
        return toVO(mapper.selectById(id));
    }

    @Override
    public Long upload(String fileName, String fileType, Long fileSize, String department) {
        DatUpload e = new DatUpload();
        e.setFileName(fileName);
        e.setFileType(fileType);
        e.setOriginalName(fileName);
        e.setFileSize(fileSize);
        e.setDepartment(department);
        e.setRowCount(0);
        e.setParsed(false);
        e.setUploadType("manual");
        mapper.insert(e);
        return e.getId();
    }

    @Override
    public Long uploadFile(MultipartFile file, String fileType, String department) {
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
        Path dayDir = UPLOAD_ROOT.resolve(LocalDate.now().toString());
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
