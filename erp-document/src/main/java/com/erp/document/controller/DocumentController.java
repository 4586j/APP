package com.erp.document.controller;
import com.erp.common.model.R; import com.erp.document.dto.*; import com.erp.document.service.DocDocumentService;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders; import org.springframework.http.MediaType; import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
@RestController @RequestMapping("/api/v1/documents") @RequiredArgsConstructor
public class DocumentController {
    final DocDocumentService service;
    @GetMapping public R<DocumentPageVO> list(DocumentQuery q){return R.ok(service.listPage(q));}
    @GetMapping("/{id}") public R<DocumentVO> get(@PathVariable Long id){return R.ok(service.getById(id));}
    @PostMapping public R<Long> create(@Valid @RequestBody DocumentCreateRequest r){return R.ok(service.create(r));}
    @PutMapping("/{id}/finalize") public R<Void> finalize(@PathVariable Long id){service.finalize(id);return R.ok();}
    @DeleteMapping("/{id}") public R<Void> delete(@PathVariable Long id){service.delete(id);return R.ok();}

    /** 上传单证文件到 MinIO */
    @PostMapping("/{id}/upload") public R<DocumentVO> upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        return R.ok(service.upload(id, file.getOriginalFilename(), file.getBytes(), file.getContentType()));
    }

    /** 生成单证 PDF 并存入 MinIO */
    @PostMapping("/{id}/generate-pdf") public R<DocumentVO> generatePdf(@PathVariable Long id){
        return R.ok(service.generatePdf(id));
    }

    /** 从 MinIO 下载单证文件 */
    @GetMapping("/{id}/download") public ResponseEntity<ByteArrayResource> download(@PathVariable Long id){
        var d=service.download(id);
        var fileName=URLEncoder.encode(d.getFileName(), StandardCharsets.UTF_8).replace("+","%20");
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''"+fileName)
            .contentType(MediaType.parseMediaType(d.getContentType()))
            .contentLength(d.getData().length)
            .body(new ByteArrayResource(d.getData()));
    }
}
