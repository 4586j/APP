/**
 * 后端统一响应包装类型 — 对应 erp-common 的 R<T>
 */
export interface ApiResult<T = any> {
  code: number
  message: string
  data: T
  timestamp?: number
}

/**
 * 分页结果 — 对应 erp-common 的 PageResult<T>
 */
export interface PageResult<T = any> {
  total: number
  pageNum: number
  pageSize: number
  records: T[]
}

/**
 * 分页查询参数 — 对应 erp-common 的 BaseQuery
 */
export interface PageQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  orderBy?: string
  orderDir?: 'asc' | 'desc'
}

/** 业务码（与后端 R.java 保持一致） */
export const BizCode = {
  SUCCESS: 200,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  CONFLICT: 409,
  SERVER_ERROR: 500,
  BIZ_ERROR: 600,
} as const
