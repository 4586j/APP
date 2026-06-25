package com.erp.report.service;

import com.erp.report.dto.*;
import com.erp.security.user.LoginUser;

public interface WorkReportService {

    /** 获取今日计划和日志 */
    WorkReportVO getToday(LoginUser user);

    /** 当月统计 */
    WorkReportStatsVO getStats(LoginUser user);

    /** 保存或更新工作计划（带时间校验） */
    Long savePlan(WorkPlanCreateRequest req, LoginUser user);

    /** 保存或更新工作日志 */
    Long saveLog(WorkLogCreateRequest req, LoginUser user);

    /** 提交计划 */
    void submitPlan(Long id, LoginUser user);

    /** 提交日志 */
    void submitLog(Long id, LoginUser user);

    /** 管理列表（合并查询） */
    WorkReportPageVO listManage(WorkReportQuery q);

    /** 批量审批 */
    void batchApprove(BatchApproveRequest req, LoginUser approver);
}
