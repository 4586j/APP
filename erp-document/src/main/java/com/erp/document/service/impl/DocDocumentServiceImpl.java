package com.erp.document.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.common.storage.MinioTemplate;
import com.erp.document.dto.*;
import com.erp.document.entity.DocDocument;
import com.erp.document.mapper.DocDocumentMapper;
import com.erp.document.service.DocDocumentService;
import com.erp.document.util.SimplePdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate; import java.time.LocalDateTime; import java.time.format.DateTimeFormatter;

@Service @RequiredArgsConstructor
public class DocDocumentServiceImpl implements DocDocumentService {
    private static final int R_NOT_FOUND = 404;
    final DocDocumentMapper mapper;
    final MinioTemplate minioTemplate;

    private DocumentVO toVO(DocDocument e){
        return DocumentVO.builder().id(e.getId()).docNo(e.getDocNo()).docType(e.getDocType()).orderId(e.getOrderId())
            .orderNo(e.getOrderNo()).shipmentId(e.getShipmentId()).title(e.getTitle()).status(e.getStatus())
            .templateCode(e.getTemplateCode()).filePath(e.getFilePath()).fileName(e.getFileName()).fileSize(e.getFileSize())
            .minioObjectKey(e.getMinioObjectKey()).contentType(e.getContentType())
            .generatedBy(e.getGeneratedBy()).generatedAt(e.getGeneratedAt()).remark(e.getRemark()).createdAt(e.getCreatedAt()).build();
    }

    @Override public DocumentPageVO listPage(DocumentQuery q){
        if(q.getPage()==null||q.getPage()<1)q.setPage(1); if(q.getSize()==null||q.getSize()<1)q.setSize(20);
        var w=new LambdaQueryWrapper<DocDocument>().eq(DocDocument::getDeleted,0);
        if(q.getDocNo()!=null) w.like(DocDocument::getDocNo,"%"+q.getDocNo()+"%");
        if(q.getDocType()!=null) w.eq(DocDocument::getDocType,q.getDocType());
        if(q.getStatus()!=null) w.eq(DocDocument::getStatus,q.getStatus());
        w.orderByDesc(DocDocument::getCreatedAt);
        var p=mapper.selectPage(new Page<>(q.getPage(),Math.min(q.getSize(),100)),w);
        var records=p.getRecords().stream().map(this::toVO).toList();
        return DocumentPageVO.builder().records(records).total(p.getTotal()).size(p.getSize()).current(p.getCurrent()).build();
    }
    @Override public DocumentVO getById(Long id){
        var e=mapper.selectById(id); if(e==null)return null;
        return toVO(e);
    }
    @Override @Transactional public Long create(DocumentCreateRequest r){
        var datePart=LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var e=new DocDocument(); e.setDocNo("DOC-"+datePart+"-"+System.currentTimeMillis()%100000);
        e.setDocType(r.getDocType()); e.setOrderId(r.getOrderId()); e.setOrderNo(r.getOrderNo()); e.setShipmentId(r.getShipmentId());
        e.setTitle(r.getTitle()); e.setTemplateCode(r.getTemplateCode()); e.setRemark(r.getRemark());
        mapper.insert(e); return e.getId();
    }
    @Override @Transactional public void finalize(Long id){
        var e=mapper.selectById(id); if(e!=null){e.setStatus("final");e.setGeneratedAt(LocalDateTime.now());mapper.updateById(e);}
    }
    @Override @Transactional public void delete(Long id){mapper.deleteById(id);}

    @Override @Transactional public DocumentVO upload(Long id, String fileName, byte[] data, String contentType){
        var e=mapper.selectById(id);
        if(e==null) throw new BusinessException(R_NOT_FOUND,"单证不存在: "+id);
        var name=(fileName==null||fileName.isBlank())?"upload.bin":fileName;
        var key=buildObjectKey(e, name);
        minioTemplate.upload(key, data, contentType);
        e.setMinioObjectKey(key); e.setFileName(name); e.setFilePath(key);
        e.setFileSize((long)data.length); e.setContentType(contentType);
        e.setGeneratedAt(LocalDateTime.now());
        mapper.updateById(e);
        return toVO(e);
    }

    @Override @Transactional public DocumentVO generatePdf(Long id){
        var e=mapper.selectById(id);
        if(e==null) throw new BusinessException(R_NOT_FOUND,"单证不存在: "+id);
        byte[] pdf=SimplePdfGenerator.generate(e);
        var fileName=e.getDocNo()+".pdf";
        var key=buildObjectKey(e, fileName);
        minioTemplate.upload(key, pdf, "application/pdf");
        e.setMinioObjectKey(key); e.setFileName(fileName); e.setFilePath(key);
        e.setFileSize((long)pdf.length); e.setContentType("application/pdf");
        e.setStatus("final"); e.setGeneratedAt(LocalDateTime.now());
        mapper.updateById(e);
        return toVO(e);
    }

    @Override public DocumentDownload download(Long id){
        var e=mapper.selectById(id);
        if(e==null) throw new BusinessException(R_NOT_FOUND,"单证不存在: "+id);
        if(e.getMinioObjectKey()==null) throw new BusinessException(400,"单证文件未生成或未上传");
        byte[] data=minioTemplate.download(e.getMinioObjectKey());
        return DocumentDownload.builder()
            .fileName(e.getFileName()==null?e.getDocNo():e.getFileName())
            .contentType(e.getContentType()==null?"application/octet-stream":e.getContentType())
            .data(data).build();
    }

    /** MinIO object key：document/{docType}/{yyyy}/{docNo}_{fileName} */
    private String buildObjectKey(DocDocument e, String fileName){
        var year=LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        return "document/"+e.getDocType()+"/"+year+"/"+e.getDocNo()+"_"+fileName;
    }
}
