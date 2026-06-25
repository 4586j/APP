package com.erp.data.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.data.dto.DataUploadPageVO; import com.erp.data.dto.DataUploadQuery;
import com.erp.data.dto.DataUploadVO; import com.erp.data.entity.DatUpload;
import com.erp.data.mapper.DatUploadMapper; import com.erp.data.service.DatUploadService;
import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service;
import java.io.Serializable;
import java.util.stream.Collectors;
@Service @RequiredArgsConstructor
public class DatUploadServiceImpl implements DatUploadService {
    final DatUploadMapper mapper;
    public DataUploadPageVO listPage(DataUploadQuery q) {
        LambdaQueryWrapper<DatUpload> w = new LambdaQueryWrapper<>();
        if (q.getKeyword() != null && !q.getKeyword().isEmpty())
            w.like(DatUpload::getFileName, q.getKeyword());
        if (q.getFileType() != null && !q.getFileType().isEmpty())
            w.eq(DatUpload::getFileType, q.getFileType());
        w.orderByDesc(DatUpload::getCreatedAt);
        Page<DatUpload> p = mapper.selectPage(new Page<>(q.getPageNum(), q.getPageSize()), w);
        return new DataUploadPageVO(p.getTotal(), q.getPageNum(), q.getPageSize(),
            p.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }
    public DataUploadVO getById(Long id) { return toVO(mapper.selectById(id)); }
    public Long upload(String fileName, String fileType, Long fileSize, String department) {
        DatUpload e = new DatUpload(); e.setFileName(fileName); e.setFileType(fileType);
        e.setOriginalName(fileName); e.setFileSize(fileSize);
        e.setDepartment(department); e.setRowCount(0);
        e.setParsed(false); e.setUploadType("manual");
        mapper.insert(e); return e.getId();
    }
    public void delete(Long id) { mapper.deleteById((Long)id); }
    private DataUploadVO toVO(DatUpload e) {
        if (e == null) return null;
        DataUploadVO v = new DataUploadVO(); v.setId(e.getId());
        v.setFileName(e.getFileName()); v.setFileType(e.getFileType());
        v.setOriginalName(e.getOriginalName()); v.setFileSize(e.getFileSize());
        v.setDepartment(e.getDepartment()); v.setRowCount(e.getRowCount());
        v.setParsed(e.getParsed()); v.setRemark(e.getRemark());
        v.setCreatedBy(e.getCreatedBy()); v.setCreatedAt(e.getCreatedAt()); return v;
    }
}