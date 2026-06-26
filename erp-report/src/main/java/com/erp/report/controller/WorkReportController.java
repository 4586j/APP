package com.erp.report.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.erp.common.model.R;
import com.erp.report.dto.*;
import com.erp.report.service.WorkReportService;
import com.erp.security.annotation.CurrentUser;
import com.erp.security.user.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/work-reports")
@RequiredArgsConstructor
@Tag(name = "工作报表")
public class WorkReportController {

    final WorkReportService service;

    /** 获取今日计划和日志 */
    @GetMapping("/today")
    @PreAuthorize("hasAnyAuthority('work:plan:view','work:log:view')")
    public R<WorkReportVO> today(@CurrentUser LoginUser user) {
        return R.ok(service.getToday(user));
    }

    /** 当月统计 */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('work:plan:view','work:log:view')")
    public R<WorkReportStatsVO> stats(@CurrentUser LoginUser user) {
        return R.ok(service.getStats(user));
    }

    /** 保存/更新工作计划 */
    @PostMapping("/plans")
    @PreAuthorize("hasAuthority('work:plan:create')")
    public R<Long> savePlan(@Valid @RequestBody WorkPlanCreateRequest req, @CurrentUser LoginUser user) {
        return R.ok(service.savePlan(req, user));
    }

    /** 保存/更新工作日志 */
    @PostMapping("/logs")
    @PreAuthorize("hasAuthority('work:log:create')")
    public R<Long> saveLog(@Valid @RequestBody WorkLogCreateRequest req, @CurrentUser LoginUser user) {
        return R.ok(service.saveLog(req, user));
    }

    /** 提交计划 */
    @PutMapping("/plans/{id}/submit")
    @PreAuthorize("hasAuthority('work:plan:submit')")
    public R<Void> submitPlan(@PathVariable Long id, @CurrentUser LoginUser user) {
        service.submitPlan(id, user);
        return R.ok();
    }

    /** 提交日志 */
    @PutMapping("/logs/{id}/submit")
    @PreAuthorize("hasAuthority('work:log:submit')")
    public R<Void> submitLog(@PathVariable Long id, @CurrentUser LoginUser user) {
        service.submitLog(id, user);
        return R.ok();
    }

    /** 管理列表（合并查询） */
    @GetMapping
    @PreAuthorize("hasAuthority('work:report:manage')")
    public R<WorkReportPageVO> list(WorkReportQuery q) {
        return R.ok(service.listManage(q));
    }

    /** 批量审批 */
    @PutMapping("/batch-approve")
    @PreAuthorize("hasAuthority('work:report:approve')")
    public R<Void> batchApprove(@Valid @RequestBody BatchApproveRequest req, @CurrentUser LoginUser user) {
        service.batchApprove(req, user);
        return R.ok();
    }
}
