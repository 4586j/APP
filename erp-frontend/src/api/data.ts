import { get, post, put, del } from './request'
import type { Id } from './system'

/* ---- 数据上传 ---- */
export interface DataUploadQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  fileType?: string
}

export interface DataUploadVO {
  id: string
  fileName: string
  fileType: string
  originalName: string
  fileSize: number
  filePath?: string
  department: string
  rowCount: number
  parsed: boolean
  remark: string
  createdBy: string
  createdAt: string
}

export interface DataUploadPageVO {
  total: number
  pageNum: number
  pageSize: number
  records: DataUploadVO[]
}

export function listUploads(params: DataUploadQuery) {
  return get<DataUploadPageVO>('/data/uploads', params)
}

export function getUpload(id: string) {
  return get<DataUploadVO>(`/data/uploads/${id}`)
}

export function createUpload(fileName: string, fileType: string, fileSize?: number, department?: string) {
  return post<Id>('/data/uploads', null, {
    params: { fileName, fileType, fileSize, department },
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

export function uploadDataFile(file: File, fileType: string, department?: string) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('fileType', fileType)
  if (department) formData.append('department', department)
  return post<Id>('/data/uploads', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteUpload(id: string) {
  return del<void>(`/data/uploads/${id}`)
}

export function downloadUpload(id: string) {
  return get<Blob>(`/data/uploads/${id}/download`, undefined, { responseType: 'blob' })
}

/* ---- 定价分析批量导入 ---- */
export interface BatchImportResult {
  successCount: number
  failList: { index: number; name: string; reason: string }[]
}

export function importPricingExcel(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return post<BatchImportResult>('/data/pricing/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function downloadPricingTemplate() {
  return get<Blob>('/data/pricing/import-template', undefined, { responseType: 'blob' })
}

/* ---- 定价分析 ---- */
export interface PricingQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  status?: string
}

export interface PricingCreateRequest {
  productId: Id
  title: string
  costPrice?: number
  targetPrice?: number
  competitorPrice?: number
  suggestedPrice?: number
  margin?: number
  marketTrend?: string
  analysisData?: string
  status?: string
  remark?: string
}

export interface PricingVO {
  id: Id
  productId: Id
  title: string
  costPrice: number
  targetPrice: number
  competitorPrice: number
  suggestedPrice: number
  margin: number
  marketTrend: string
  status: string
  remark: string
  createdBy: string
  createdAt: string
}

export interface PricingPageVO {
  total: number
  pageNum: number
  pageSize: number
  records: PricingVO[]
}

export function listPricings(params: PricingQuery) {
  return get<PricingPageVO>('/data/pricing', params)
}

export function getPricing(id: Id) {
  return get<PricingVO>(`/data/pricing/${id}`)
}

export function createPricing(data: PricingCreateRequest) {
  return post<Id>('/data/pricing', data)
}

export function updatePricing(id: Id, data: PricingCreateRequest) {
  return put<void>(`/data/pricing/${id}`, data)
}

export function deletePricing(id: Id) {
  return del<void>(`/data/pricing/${id}`)
}

/* ---- 仪表盘 ---- */
export interface DashboardStatsVO {
  customerCount: number
  productCount: number
  orderCount: number
  monthlyRevenue: number
  monthlyProfit: number
  trend: { month: string; revenue: number; profit: number }[]
  orderStatusDist: { name: string; value: number }[]
}

export function getDashboardStats() {
  return get<DashboardStatsVO>('/dashboard/stats')
}
