package com.erp.data.controller;

import com.erp.common.model.R;
import com.erp.data.dto.DatFileQuery;
import com.erp.data.dto.DatFileVO;
import com.erp.data.service.DatFileService;
import com.erp.security.annotation.CurrentUser;
import com.erp.security.user.LoginUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "企业网盘")
public class DatFileController {

    private final DatFileService service;

    /**
     * 列出目录内容。
     */
    @GetMapping
    public R<List<DatFileVO>> list(DatFileQuery q, @CurrentUser LoginUser user) {
        return R.ok(service.listFiles(q, user));
    }

    /**
     * 获取面包屑路径。
     */
    @GetMapping("/breadcrumb")
    public R<List<DatFileVO>> breadcrumb(@RequestParam(required = false) Long fileId) {
        return R.ok(service.getBreadcrumb(fileId));
    }

    /**
     * 新建文件夹。
     */
    @PostMapping("/folder")
    @PreAuthorize("hasAuthority('data:upload:create')")
    public R<Long> createFolder(@RequestParam(required = false) Long parentId,
                                 @RequestParam String name,
                                 @CurrentUser LoginUser user) {
        return R.ok(service.createFolder(parentId, name, null, user));
    }

    /**
     * 上传文件。
     */
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('data:upload:create')")
    public R<Long> upload(@RequestParam("file") MultipartFile file,
                           @RequestParam(required = false) Long parentId,
                           @RequestParam(required = false) String fileType,
                           @RequestParam(required = false) Long deptId,
                           @RequestParam(required = false) String shareDeptIds,
                           @CurrentUser LoginUser user) {
        List<Long> ids = parseShareDeptIds(shareDeptIds);
        return R.ok(service.uploadFile(file, parentId, fileType, deptId, ids, user));
    }

    /**
     * 重命名。
     */
    @PutMapping("/{id}/rename")
    @PreAuthorize("hasAuthority('data:upload:create')")
    public R<Void> rename(@PathVariable Long id,
                           @RequestParam String name,
                           @CurrentUser LoginUser user) {
        service.rename(id, name, user);
        return R.ok();
    }

    /**
     * 移动。
     */
    @PutMapping("/{id}/move")
    @PreAuthorize("hasAuthority('data:upload:create')")
    public R<Void> move(@PathVariable Long id,
                         @RequestParam(required = false) Long targetParentId,
                         @CurrentUser LoginUser user) {
        service.move(id, targetParentId, user);
        return R.ok();
    }

    /**
     * 删除（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('data:upload:delete')")
    public R<Void> delete(@PathVariable Long id, @CurrentUser LoginUser user) {
        service.delete(id, user);
        return R.ok();
    }

    /**
     * 下载文件。
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('data:upload:download')")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        service.download(id, response);
    }

    /** 解析逗号分隔的共享部门 ID 字符串。 */
    private List<Long> parseShareDeptIds(String shareDeptIds) {
        if (shareDeptIds == null || shareDeptIds.isBlank()) return List.of();
        return java.util.Arrays.stream(shareDeptIds.split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf)
            .collect(java.util.stream.Collectors.toList());
    }
}
