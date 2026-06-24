import axios, { type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { storage, StorageKey } from '@/utils/storage'
import type { ApiResult } from '@/types/api'
import { BizCode } from '@/types/api'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json;charset=utf-8' },
})

// 请求拦截：注入 token + traceId
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = storage.get<string>(StorageKey.TOKEN)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 简易 traceId（便于排错）
    config.headers['X-Trace-Id'] = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`
    return config
  },
  (err) => Promise.reject(err),
)

// 响应拦截：拆 R<T>、统一 401 跳转、错误 toast
let isReloginPrompted = false

service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResult

    // 二进制流（导出 / 文件下载）直接返回 raw
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response
    }

    if (res.code === BizCode.SUCCESS) {
      return res.data
    }

    // 401 单独处理
    if (res.code === BizCode.UNAUTHORIZED) {
      promptRelogin(res.message || '登录已过期，请重新登录')
      return Promise.reject(new Error(res.message))
    }

    // 其他业务错误统一 toast
    ElMessage({ message: res.message || '请求失败', type: 'error', duration: 3000 })
    return Promise.reject(new Error(res.message || 'Error'))
  },
  (error) => {
    const status = error.response?.status
    const msg = error.response?.data?.message || error.message || '网络异常'

    if (status === 401) {
      promptRelogin('登录已过期，请重新登录')
    } else if (status === 403) {
      ElMessage({ message: '没有权限访问该资源', type: 'warning' })
    } else if (status >= 500) {
      ElMessage({ message: `服务器错误 (${status})`, type: 'error' })
    } else {
      ElMessage({ message: msg, type: 'error', duration: 3000 })
    }

    return Promise.reject(error)
  },
)

function promptRelogin(msg: string) {
  if (isReloginPrompted) return
  isReloginPrompted = true
  ElMessageBox.confirm(msg, '提示', {
    confirmButtonText: '重新登录',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      storage.clear()
      window.location.href = '/login'
    })
    .finally(() => {
      isReloginPrompted = false
    })
}

/** 类型化 request helpers（业务层只用这些） */
export function get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, { params, ...config })
}
export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, data, config)
}
export function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.put(url, data, config)
}
export function del<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.delete(url, config)
}

export default service
