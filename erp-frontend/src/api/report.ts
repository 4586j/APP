import { get, post, put } from './request'
import type { Id } from './system'

/* ---- 工作台今日数据 ---- */
export interface WorkReportVO {
  userId: Id
  username: string
  realName: string
  departmentName: string
  reportDate: string
  planId?: Id
  planTitle?: string
  planContent?: string
  planStatus?: string
  planCreatedAt?: string
  planApproveTime?: string
  planApproveComment?: string
  logId?: Id
  logTitle?: string
  logContent?: string
  logStatus?: string
  logCreatedAt?: string
  logApproveTime?: string
  logApproveComment?: string
}

export function getTodayReport() {
  return get<WorkReportVO>('/work-reports/today')
}

/* ---- 统计 ---- */
export interface WorkReportStatsVO {
  planCount: number
  logCount: number
  rejectedCount: number
}

export function getReportStats() {
  return get<WorkReportStatsVO>('/work-reports/stats')
}

/* ---- 计划 ---- */
export interface WorkPlanCreateRequest {
  id?: Id
  reportDate: string
  title?: string
  content: string
}

export function saveWorkPlan(data: WorkPlanCreateRequest) {
  return post<Id>('/work-reports/plans', data)
}

export function submitWorkPlan(id: Id) {
  return put<void>(`/work-reports/plans/${id}/submit`)
}

/* ---- 日志 ---- */
export interface WorkLogCreateRequest {
  id?: Id
  reportDate: string
  title?: string
  content: string
}

export function saveWorkLog(data: WorkLogCreateRequest) {
  return post<Id>('/work-reports/logs', data)
}

export function submitWorkLog(id: Id) {
  return put<void>(`/work-reports/logs/${id}/submit`)
}

/* ---- 管理列表 ---- */
export interface WorkReportQuery {
  reportDate?: string
  departmentId?: Id
  keyword?: string
  status?: string
  type?: string
  page?: number
  size?: number
}

export interface WorkReportPageVO {
  records: WorkReportVO[]
  total: number
  size: number
  current: number
}

export function listWorkReports(params: WorkReportQuery) {
  return get<WorkReportPageVO>('/work-reports', params)
}

/* ---- 批量审批 ---- */
export interface BatchApproveRequest {
  planIds?: Id[]
  logIds?: Id[]
  action: 'approved' | 'rejected'
  comment?: string
}

export function batchApproveWorkReports(data: BatchApproveRequest) {
  return put<void>('/work-reports/batch-approve', data)
}
