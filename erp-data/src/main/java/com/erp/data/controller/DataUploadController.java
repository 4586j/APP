package com.erp.data.controller;

import com.erp.common.model.R;
import com.erp.data.dto.DataUploadPageVO;
import com.erp.data.dto.DataUploadQuery;
import com.erp.data.dto.DataUploadVO;
import com.erp.data.service.DatUploadService;
import com.erp.security.annotation.CurrentUser;
import com.erp.security.user.LoginUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/data/uploads")
@RequiredArgsConstructor
@Tag(name = "数据上传")
public class DataUploadController {
    private final DatUploadService service;

    @GetMapping
    public R<DataUploadPageVO> list(DataUploadQuery q, @CurrentUser LoginUser user) {
        return R.ok(service.listPage(q, user));
    }

    @GetMapping("/{id}")
    public R<DataUploadVO> get(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    /**
     * 下载上传的文件。
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('data:upload:download')")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        service.download(id, response);
    }

    /**
     * 真实文件上传：前端选择本机文件后以 multipart/form-data 提交。
     */
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('data:upload:create')")
    public R<Long> uploadFile(@RequestParam("file") MultipartFile file,
                              @RequestParam String fileType,
                              @RequestParam(required = false) String department,
                              @CurrentUser LoginUser user) {
        return R.ok(service.uploadFile(file, fileType, department, user.getId()));
    }

    /**
     * 兼容旧接口/自动化脚本：只写上传元数据，不保存实体文件。
     */
    @PostMapping(consumes = "application/x-www-form-urlencoded")
    @PreAuthorize("hasAuthority('data:upload:create')")
    public R<Long> create(@RequestParam String fileName,
                          @RequestParam String fileType,
                          @RequestParam(required = false) Long fileSize,
                          @RequestParam(required = false) String department,
                          @CurrentUser LoginUser user) {
        return R.ok(service.upload(fileName, fileType, fileSize != null ? fileSize : 0L, department, user.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('data:upload:delete')")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }
}
