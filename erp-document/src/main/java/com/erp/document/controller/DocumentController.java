package com.erp.document.controller;
import com.erp.common.model.R; import com.erp.document.dto.*; import com.erp.document.service.DocDocumentService;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/documents") @RequiredArgsConstructor
public class DocumentController {
    final DocDocumentService service;
    @GetMapping public R<DocumentPageVO> list(DocumentQuery q){return R.ok(service.listPage(q));}
    @GetMapping("/{id}") public R<DocumentVO> get(@PathVariable Long id){return R.ok(service.getById(id));}
    @PostMapping public R<Long> create(@Valid @RequestBody DocumentCreateRequest r){return R.ok(service.create(r));}
    @PutMapping("/{id}/finalize") public R<Void> finalize(@PathVariable Long id){service.finalize(id);return R.ok();}
    @DeleteMapping("/{id}") public R<Void> delete(@PathVariable Long id){service.delete(id);return R.ok();}
}