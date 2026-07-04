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
  deptId?: string
  shareDeptIds?: string[]
  shareDeptNames?: string[]
  rowCount: number
  parsed: boolean
  remark: string
  createdBy: string
  createdByName?: string
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

export function createUpload(fileName: string, fileType: string, fileSize?: number, department?: string, deptId?: string, shareDeptIds?: string) {
  return post<Id>('/data/uploads', null, {
    params: { fileName, fileType, fileSize, department, deptId, shareDeptIds },
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

export function uploadDataFile(file: File, fileType: string, department?: string, deptId?: string, shareDeptIds?: string, onProgress?: (percent: number) => void) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('fileType', fileType)
  if (department) formData.append('department', department)
  if (deptId) formData.append('deptId', deptId)
  if (shareDeptIds) formData.append('shareDeptIds', shareDeptIds)
  return post<Id>('/data/uploads', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 0,
    onUploadProgress: (progressEvent: any) => {
      if (onProgress && progressEvent.total) {
        onProgress(Math.round((progressEvent.loaded * 100) / progressEvent.total))
      }
    },
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

export interface ImportTaskVO {
  taskId: string
  fileName: string
  status: 'PENDING' | 'RUNNING' | 'DONE' | 'FAILED'
  message: string
  totalRows: number
  processedRows: number
  successCount: number
  failCount: number
  percent: number
  failList: { index: number; name: string; reason: string }[]
  createdAt: string
  finishedAt?: string
}

export function importPricingExcel(file: File, onProgress?: (percent: number) => void) {
  const formData = new FormData()
  formData.append('file', file)
  return post<ImportTaskVO>('/data/pricing/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 0,
    onUploadProgress: (progressEvent: any) => {
      if (onProgress && progressEvent.total) {
        onProgress(Math.round((progressEvent.loaded * 100) / progressEvent.total))
      }
    },
  })
}

export function getImportProgress(taskId: string) {
  return get<ImportTaskVO>(`/data/pricing/import/${taskId}/progress`)
}

/* ---- 企业网盘 ---- */
export interface DatFileQuery {
  parentId?: string
  deptId?: string
  keyword?: string
  fileType?: string
  pageNum?: number
  pageSize?: number
}

export interface DatFileVO {
  id: string
  parentId?: string
  isDirectory: number
  name: string
  displayName?: string
  extension?: string
  mimeType?: string
  fileSize: number
  fileType?: string
  department?: string
  deptId?: string
  shareDeptIds?: string[]
  shareDeptNames?: string[]
  rowCount?: number
  parsed?: boolean
  remark?: string
  createdBy: string
  createdByName?: string
  createdAt: string
  updatedBy?: string
  updatedAt?: string
}

export function listFiles(params: DatFileQuery) {
  return get<DatFileVO[]>('/files', params)
}

export function getBreadcrumb(fileId?: string) {
  return get<DatFileVO[]>('/files/breadcrumb', { fileId })
}

export function createFolder(parentId: string | undefined, name: string, deptId?: string, shareDeptIds?: string) {
  return post<string>('/files/folder', null, { params: { parentId, name, deptId, shareDeptIds } })
}

export function uploadFileToNetdisk(file: File, parentId: string | undefined, fileType: string | undefined, deptId: string | undefined, shareDeptIds: string | undefined, onProgress?: (percent: number) => void) {
  const formData = new FormData()
  // 第三参数显式指定纯文件名：webkitdirectory 选出的 File，浏览器默认会用
  // webkitRelativePath（如 "决策树/c45.py"）作为 multipart filename，导致后端
  // display_name 含斜杠，WebDAV PROPFIND 拼 href 时 / 被编码成 %2F 而无法显示。
  // 强制用 file.name（纯名）从源头消除 display_name 污染。
  formData.append('file', file, file.name)
  if (parentId) formData.append('parentId', parentId)
  if (fileType) formData.append('fileType', fileType)
  if (deptId) formData.append('deptId', deptId)
  if (shareDeptIds) formData.append('shareDeptIds', shareDeptIds)
  return post<string>('/files', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 0,
    onUploadProgress: (progressEvent: any) => {
      if (onProgress && progressEvent.total) {
        onProgress(Math.round((progressEvent.loaded * 100) / progressEvent.total))
      }
    },
  })
}

export function renameFile(id: string, name: string) {
  return put<void>(`/files/${id}/rename`, null, { params: { name } })
}

export function moveFile(id: string, targetParentId: string | undefined) {
  return put<void>(`/files/${id}/move`, null, { params: { targetParentId } })
}

export function deleteFile(id: string) {
  return del<void>(`/files/${id}`)
}

export function downloadFile(id: string) {
  return get<Blob>(`/files/${id}/download`, undefined, { responseType: 'blob' })
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
