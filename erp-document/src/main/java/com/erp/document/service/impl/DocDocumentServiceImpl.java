package com.erp.document.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.document.dto.*;
import com.erp.document.entity.DocDocument;
import com.erp.document.mapper.DocDocumentMapper;
import com.erp.document.service.DocDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate; import java.time.LocalDateTime; import java.time.format.DateTimeFormatter;

@Service @RequiredArgsConstructor
public class DocDocumentServiceImpl implements DocDocumentService {
    final DocDocumentMapper mapper;
    @Override public DocumentPageVO listPage(DocumentQuery q){
        if(q.getPage()==null||q.getPage()<1)q.setPage(1); if(q.getSize()==null||q.getSize()<1)q.setSize(20);
        var w=new LambdaQueryWrapper<DocDocument>().eq(DocDocument::getDeleted,0);
        if(q.getDocNo()!=null) w.like(DocDocument::getDocNo,"%"+q.getDocNo()+"%");
        if(q.getDocType()!=null) w.eq(DocDocument::getDocType,q.getDocType());
        if(q.getStatus()!=null) w.eq(DocDocument::getStatus,q.getStatus());
        w.orderByDesc(DocDocument::getCreatedAt);
        var p=mapper.selectPage(new Page<>(q.getPage(),Math.min(q.getSize(),100)),w);
        var records=p.getRecords().stream().map(e->DocumentVO.builder().id(e.getId()).docNo(e.getDocNo())
            .docType(e.getDocType()).orderId(e.getOrderId()).orderNo(e.getOrderNo()).shipmentId(e.getShipmentId())
            .title(e.getTitle()).status(e.getStatus()).templateCode(e.getTemplateCode())
            .filePath(e.getFilePath()).fileName(e.getFileName()).fileSize(e.getFileSize())
            .generatedBy(e.getGeneratedBy()).generatedAt(e.getGeneratedAt()).remark(e.getRemark()).createdAt(e.getCreatedAt()).build()).toList();
        return DocumentPageVO.builder().records(records).total(p.getTotal()).size(p.getSize()).current(p.getCurrent()).build();
    }
    @Override public DocumentVO getById(Long id){
        var e=mapper.selectById(id); if(e==null)return null;
        return DocumentVO.builder().id(e.getId()).docNo(e.getDocNo()).docType(e.getDocType()).orderId(e.getOrderId())
            .orderNo(e.getOrderNo()).shipmentId(e.getShipmentId()).title(e.getTitle()).status(e.getStatus())
            .templateCode(e.getTemplateCode()).filePath(e.getFilePath()).fileName(e.getFileName()).fileSize(e.getFileSize())
            .generatedBy(e.getGeneratedBy()).generatedAt(e.getGeneratedAt()).remark(e.getRemark()).createdAt(e.getCreatedAt()).build();
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
}