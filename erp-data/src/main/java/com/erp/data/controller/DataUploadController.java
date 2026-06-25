package com.erp.data.controller;
import com.erp.common.model.R; import com.erp.data.dto.DataUploadPageVO;
import com.erp.data.dto.DataUploadQuery; import com.erp.data.dto.DataUploadVO;
import com.erp.data.service.DatUploadService;
import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/data/uploads") @RequiredArgsConstructor
public class DataUploadController {
    final DatUploadService service;
    @GetMapping public R<DataUploadPageVO> list(DataUploadQuery q){return R.ok(service.listPage(q));}
    @GetMapping("/{id}") public R<DataUploadVO> get(@PathVariable Long id){return R.ok(service.getById(id));}
    @PostMapping public R<Long> create(@RequestParam String fileName,@RequestParam String fileType,
        @RequestParam(required=false) Long fileSize,@RequestParam(required=false) String department){
        return R.ok(service.upload(fileName,fileType,fileSize!=null?fileSize:0L,department));}
    @DeleteMapping("/{id}") public R<Void> delete(@PathVariable Long id){service.delete(id);return R.ok();}
}