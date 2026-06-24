import { get, post, put, del } from './request'
import type { PageQuery, PageResult } from '@/types/api'
import type { UserInfo } from '@/types/auth'

/** 用户列表 */
export function listUsers(params: PageQuery) {
  return get<PageResult<UserInfo>>('/system/users', params)
}

export function getUser(id: number) {
  return get<UserInfo>(`/system/users/${id}`)
}

export function createUser(data: Partial<UserInfo> & { password: string }) {
  return post<UserInfo>('/system/users', data)
}

export function updateUser(id: number, data: Partial<UserInfo>) {
  return put<UserInfo>(`/system/users/${id}`, data)
}

export function deleteUser(id: number) {
  return del<void>(`/system/users/${id}`)
}

/** 角色 / 权限 */
export interface Role {
  id: number
  code: string
  name: string
  remark?: string
  permissionCodes: string[]
}

export function listRoles(params: PageQuery) {
  return get<PageResult<Role>>('/system/roles', params)
}

export function listAllPermissions() {
  return get<string[]>('/system/permissions')
}
