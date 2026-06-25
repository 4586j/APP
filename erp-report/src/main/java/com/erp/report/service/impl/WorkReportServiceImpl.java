package com.erp.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.notification.service.NotificationService;
import com.erp.report.dto.*;
import com.erp.report.entity.RptWorkLog;
import com.erp.report.entity.RptWorkPlan;
import com.erp.report.mapper.RptWorkLogMapper;
import com.erp.report.mapper.RptWorkPlanMapper;
import com.erp.report.service.WorkReportService;
import com.erp.security.user.LoginUser;
import com.erp.user.entity.SysUser;
import com.erp.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkReportServiceImpl implements WorkReportService {

    final RptWorkPlanMapper planMapper;
    final RptWorkLogMapper logMapper;
    final SysUserMapper userMapper;
    final NotificationService notificationService;

    private static final LocalTime PLAN_START = LocalTime.of(8, 30);
    private static final LocalTime PLAN_END = LocalTime.of(10, 30);

    @Override
    public WorkReportVO getToday(LoginUser user) {
        Long uid = user.getId();
        LocalDate today = LocalDate.now();
        RptWorkPlan plan = planMapper.selectOne(
            new LambdaQueryWrapper<RptWorkPlan>()
                .eq(RptWorkPlan::getUserId, uid)
                .eq(RptWorkPlan::getReportDate, today)
        );
        RptWorkLog log = logMapper.selectOne(
            new LambdaQueryWrapper<RptWorkLog>()
                .eq(RptWorkLog::getUserId, uid)
                .eq(RptWorkLog::getReportDate, today)
        );
        return buildWorkReportVO(user, plan, log, today);
    }

    @Override
    public WorkReportStatsVO getStats(LoginUser user) {
        Long uid = user.getId();
        YearMonth ym = YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        long planCount = planMapper.selectCount(
            new LambdaQueryWrapper<RptWorkPlan>()
                .eq(RptWorkPlan::getUserId, uid)
                .ge(RptWorkPlan::getReportDate, start)
                .le(RptWorkPlan::getReportDate, end)
                .eq(RptWorkPlan::getStatus, "submitted")
        );
        long logCount = logMapper.selectCount(
            new LambdaQueryWrapper<RptWorkLog>()
                .eq(RptWorkLog::getUserId, uid)
                .ge(RptWorkLog::getReportDate, start)
                .le(RptWorkLog::getReportDate, end)
                .eq(RptWorkLog::getStatus, "submitted")
        );
        long rejectedCount = planMapper.selectCount(
            new LambdaQueryWrapper<RptWorkPlan>()
                .eq(RptWorkPlan::getUserId, uid)
                .ge(RptWorkPlan::getReportDate, start)
                .le(RptWorkPlan::getReportDate, end)
                .eq(RptWorkPlan::getStatus, "rejected")
        ) + logMapper.selectCount(
            new LambdaQueryWrapper<RptWorkLog>()
                .eq(RptWorkLog::getUserId, uid)
                .ge(RptWorkLog::getReportDate, start)
                .le(RptWorkLog::getReportDate, end)
                .eq(RptWorkLog::getStatus, "rejected")
        );

        return WorkReportStatsVO.builder()
            .planCount(planCount)
            .logCount(logCount)
            .rejectedCount(rejectedCount)
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePlan(WorkPlanCreateRequest req, LoginUser user) {
        Long uid = user.getId();
        LocalDate date = req.getReportDate();

        // 时间校验：只能在 8:30-10:30 创建/修改计划
        LocalTime now = LocalTime.now();
        if (now.isBefore(PLAN_START) || now.isAfter(PLAN_END)) {
            throw new BusinessException(R.CODE_PARAM_INVALID, "工作计划只能在 08:30-10:30 填写");
        }

        // 查询是否已存在
        RptWorkPlan exist = planMapper.selectOne(
            new LambdaQueryWrapper<RptWorkPlan>()
                .eq(RptWorkPlan::getUserId, uid)
                .eq(RptWorkPlan::getReportDate, date)
        );

        if (exist != null) {
            // 已提交且超时后不允许修改内容
            if (!"draft".equals(exist.getStatus())) {
                throw new BusinessException(R.CODE_PARAM_INVALID, "已提交的计划无法修改");
            }
            exist.setTitle(req.getTitle());
            exist.setContent(req.getContent());
            planMapper.updateById(exist);
            return exist.getId();
        }

        Long deptId = getDepartmentId(uid);
        RptWorkPlan plan = new RptWorkPlan();
        plan.setUserId(uid);
        plan.setDepartmentId(deptId);
        plan.setReportDate(date);
        plan.setTitle(req.getTitle());
        plan.setContent(req.getContent());
        plan.setStatus("draft");
        planMapper.insert(plan);
        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveLog(WorkLogCreateRequest req, LoginUser user) {
        Long uid = user.getId();
        LocalDate date = req.getReportDate();

        RptWorkLog exist = logMapper.selectOne(
            new LambdaQueryWrapper<RptWorkLog>()
                .eq(RptWorkLog::getUserId, uid)
                .eq(RptWorkLog::getReportDate, date)
        );

        if (exist != null) {
            if (!"draft".equals(exist.getStatus()) && !"rejected".equals(exist.getStatus())) {
                throw new BusinessException(R.CODE_PARAM_INVALID, "已审批通过的日志无法修改");
            }
            exist.setTitle(req.getTitle());
            exist.setContent(req.getContent());
            logMapper.updateById(exist);
            return exist.getId();
        }

        Long deptId = getDepartmentId(uid);
        RptWorkLog log = new RptWorkLog();
        log.setUserId(uid);
        log.setDepartmentId(deptId);
        log.setReportDate(date);
        log.setTitle(req.getTitle());
        log.setContent(req.getContent());
        log.setStatus("draft");
        logMapper.insert(log);
        return log.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitPlan(Long id, LoginUser user) {
        RptWorkPlan plan = planMapper.selectById(id);
        if (plan == null || !plan.getUserId().equals(user.getId())) {
            throw new BusinessException(R.CODE_NOT_FOUND, "计划不存在");
        }
        LocalTime now = LocalTime.now();
        if (now.isBefore(PLAN_START) || now.isAfter(PLAN_END)) {
            throw new BusinessException(R.CODE_PARAM_INVALID, "计划提交只能在 08:30-10:30 进行");
        }
        plan.setStatus("submitted");
        planMapper.updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitLog(Long id, LoginUser user) {
        RptWorkLog log = logMapper.selectById(id);
        if (log == null || !log.getUserId().equals(user.getId())) {
            throw new BusinessException(R.CODE_NOT_FOUND, "日志不存在");
        }
        log.setStatus("submitted");
        logMapper.updateById(log);
    }

    @Override
    public WorkReportPageVO listManage(WorkReportQuery q) {
        // 获取符合条件的用户列表
        List<SysUser> users = fetchUsers(q);
        if (users.isEmpty()) {
            return WorkReportPageVO.builder().records(List.of()).total(0).size(q.getSize()).current(q.getPage()).build();
        }

        List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());
        LocalDate date = q.getReportDate() != null ? q.getReportDate() : LocalDate.now();

        // 批量查询计划和日志
        List<RptWorkPlan> plans = planMapper.selectList(
            new LambdaQueryWrapper<RptWorkPlan>()
                .in(RptWorkPlan::getUserId, userIds)
                .eq(RptWorkPlan::getReportDate, date)
                .eq(q.getStatus() != null, RptWorkPlan::getStatus, q.getStatus())
        );
        List<RptWorkLog> logs = logMapper.selectList(
            new LambdaQueryWrapper<RptWorkLog>()
                .in(RptWorkLog::getUserId, userIds)
                .eq(RptWorkLog::getReportDate, date)
                .eq(q.getStatus() != null, RptWorkLog::getStatus, q.getStatus())
        );

        Map<Long, RptWorkPlan> planMap = plans.stream().collect(Collectors.toMap(RptWorkPlan::getUserId, p -> p));
        Map<Long, RptWorkLog> logMap = logs.stream().collect(Collectors.toMap(RptWorkLog::getUserId, l -> l));

        List<WorkReportVO> records = users.stream().map(u -> {
            RptWorkPlan p = planMap.get(u.getId());
            RptWorkLog l = logMap.get(u.getId());
            return buildWorkReportVO(u, p, l, date);
        }).collect(Collectors.toList());

        // 按 type 过滤：只显示有 plan 或 log 的记录
        String type = q.getType();
        if ("plan".equals(type)) {
            records = records.stream().filter(r -> r.getPlanId() != null).collect(Collectors.toList());
        } else if ("log".equals(type)) {
            records = records.stream().filter(r -> r.getLogId() != null).collect(Collectors.toList());
        }

        // 内存分页
        int total = records.size();
        int start = (q.getPage() - 1) * q.getSize();
        int end = Math.min(start + q.getSize(), total);
        List<WorkReportVO> pageRecords = start < total ? records.subList(start, end) : List.of();

        return WorkReportPageVO.builder()
            .records(pageRecords)
            .total(total)
            .size(q.getSize())
            .current(q.getPage())
            .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchApprove(BatchApproveRequest req, LoginUser approver) {
        String action = req.getAction();
        String comment = req.getComment();
        LocalDateTime now = LocalDateTime.now();

        if (req.getPlanIds() != null) {
            for (Long id : req.getPlanIds()) {
                RptWorkPlan plan = planMapper.selectById(id);
                if (plan != null && "submitted".equals(plan.getStatus())) {
                    plan.setStatus(action);
                    plan.setApproverId(approver.getId());
                    plan.setApproveComment(comment);
                    plan.setApproveTime(now);
                    planMapper.updateById(plan);
                    notifyUser(plan.getUserId(), "工作计划" + ("approved".equals(action) ? "已通过" : "被驳回"),
                        "您的工作计划已被" + ("approved".equals(action) ? "通过" : "驳回") +
                        (comment != null ? "，意见：" + comment : ""));
                }
            }
        }
        if (req.getLogIds() != null) {
            for (Long id : req.getLogIds()) {
                RptWorkLog log = logMapper.selectById(id);
                if (log != null && "submitted".equals(log.getStatus())) {
                    log.setStatus(action);
                    log.setApproverId(approver.getId());
                    log.setApproveComment(comment);
                    log.setApproveTime(now);
                    logMapper.updateById(log);
                    notifyUser(log.getUserId(), "工作日志" + ("approved".equals(action) ? "已通过" : "被驳回"),
                        "您的工作日志已被" + ("approved".equals(action) ? "通过" : "驳回") +
                        (comment != null ? "，意见：" + comment : ""));
                }
            }
        }
    }

    // ---------- 私有方法 ----------

    private Long getDepartmentId(Long userId) {
        SysUser user = userMapper.selectById(userId);
        return user != null ? user.getDepartmentId() : null;
    }

    private List<SysUser> fetchUsers(WorkReportQuery q) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        if (q.getDepartmentId() != null) {
            w.eq(SysUser::getDepartmentId, q.getDepartmentId());
        }
        if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
            String kw = "%" + q.getKeyword() + "%";
            w.and(ww -> ww.like(SysUser::getUsername, kw).or().like(SysUser::getRealName, kw));
        }
        w.orderByAsc(SysUser::getId);
        return userMapper.selectList(w);
    }

    private WorkReportVO buildWorkReportVO(LoginUser user, RptWorkPlan plan, RptWorkLog log, LocalDate date) {
        return WorkReportVO.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .realName(user.getRealName())
            .departmentName(user.getDepartmentName())
            .reportDate(date)
            .planId(plan != null ? plan.getId() : null)
            .planTitle(plan != null ? plan.getTitle() : null)
            .planContent(plan != null ? plan.getContent() : null)
            .planStatus(plan != null ? plan.getStatus() : null)
            .planCreatedAt(plan != null ? plan.getCreatedAt() : null)
            .planApproveTime(plan != null ? plan.getApproveTime() : null)
            .planApproveComment(plan != null ? plan.getApproveComment() : null)
            .logId(log != null ? log.getId() : null)
            .logTitle(log != null ? log.getTitle() : null)
            .logContent(log != null ? log.getContent() : null)
            .logStatus(log != null ? log.getStatus() : null)
            .logCreatedAt(log != null ? log.getCreatedAt() : null)
            .logApproveTime(log != null ? log.getApproveTime() : null)
            .logApproveComment(log != null ? log.getApproveComment() : null)
            .build();
    }

    private WorkReportVO buildWorkReportVO(SysUser user, RptWorkPlan plan, RptWorkLog log, LocalDate date) {
        return WorkReportVO.builder()
            .userId(user.getId())
            .username(user.getUsername())
            .realName(user.getRealName())
            .reportDate(date)
            .planId(plan != null ? plan.getId() : null)
            .planTitle(plan != null ? plan.getTitle() : null)
            .planContent(plan != null ? plan.getContent() : null)
            .planStatus(plan != null ? plan.getStatus() : null)
            .planCreatedAt(plan != null ? plan.getCreatedAt() : null)
            .planApproveTime(plan != null ? plan.getApproveTime() : null)
            .planApproveComment(plan != null ? plan.getApproveComment() : null)
            .logId(log != null ? log.getId() : null)
            .logTitle(log != null ? log.getTitle() : null)
            .logContent(log != null ? log.getContent() : null)
            .logStatus(log != null ? log.getStatus() : null)
            .logCreatedAt(log != null ? log.getCreatedAt() : null)
            .logApproveTime(log != null ? log.getApproveTime() : null)
            .logApproveComment(log != null ? log.getApproveComment() : null)
            .build();
    }

    private void notifyUser(Long userId, String title, String content) {
        try {
            notificationService.send(title, content, "work_report", "work_report", null, userId);
        } catch (Exception e) {
            // 通知失败不影响主流程
        }
    }
}
