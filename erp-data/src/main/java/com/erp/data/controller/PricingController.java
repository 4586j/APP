package com.erp.data.controller;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.erp.common.dto.BatchImportResult;
import com.erp.common.model.R; import com.erp.data.dto.PricingPageVO;
import com.erp.data.dto.PricingQuery; import com.erp.data.dto.PricingVO;
import com.erp.data.dto.PricingCreateRequest;
import com.erp.data.dto.PricingImportExcelDTO;
import com.erp.data.service.PricingService;
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController @RequestMapping("/api/v1/data/pricing") @RequiredArgsConstructor
@Tag(name = "定价分析")
public class PricingController {
    final PricingService service;
    @GetMapping public R<PricingPageVO> list(PricingQuery q){return R.ok(service.listPage(q));}
    @GetMapping("/{id}") public R<PricingVO> get(@PathVariable Long id){return R.ok(service.getById(id));}
    @PostMapping public R<Long> create(@Valid @RequestBody PricingCreateRequest r){return R.ok(service.create(r));}
    @PutMapping("/{id}") public R<Void> update(@PathVariable Long id,@Valid @RequestBody PricingCreateRequest r){service.update(id,r);return R.ok();}
    @DeleteMapping("/{id}") public R<Void> delete(@PathVariable Long id){service.delete(id);return R.ok();}

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('data:pricing:import')")
    public R<BatchImportResult> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return R.ok(service.batchImport(file.getInputStream()));
    }

    @GetMapping("/import-template")
    @PreAuthorize("hasAuthority('data:pricing:import')")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("定价分析导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), PricingImportExcelDTO.class)
            .sheet("定价分析模板")
            .doWrite(List.of());
    }
}