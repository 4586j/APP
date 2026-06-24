import { get, post, put, del } from './request'

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages?: number
}

export interface SystemUser {
  id: number
  username: string
  realName: string
  email?: string
  phone?: string
  avatarUrl?: string
  departmentId?: number
  departmentName?: string
  superiorId?: number
  status: number
  pwdResetRequired?: number
  lastLoginTime?: string
  lastLoginIp?: string
  lockedUntil?: string
  createdAt?: string
  roleCodes?: string[]
  permCodes?: string[]
}

export interface UserQuery {
  page?: number
  size?: number
  username?: string
  realName?: string
  departmentId?: number
  status?: number
}

export interface UserCreateRequest {
  username: string
  password: string
  realName: string
  email?: string
  phone?: string
  departmentId?: number
  superiorId?: number
  roleIds?: number[]
  status?: number
}

export interface UserUpdateRequest {
  realName?: string
  email?: string
  phone?: string
  avatarUrl?: string
  departmentId?: number
  superiorId?: number
  status?: number
}

export interface Role {
  id: number
  roleName: string
  roleCode: string
  dataScope?: number
  description?: string
  status?: number
  createdAt?: string
  permissionIds?: number[]
}

export interface PermissionNode {
  id: number
  parentId: number
  name: string
  code: string
  type: 'menu' | 'button' | 'api'
  httpMethod?: string
  icon?: string
  path?: string
  component?: string
  sortOrder?: number
  status?: number
  children?: PermissionNode[]
}

export interface DepartmentNode {
  id: number
  parentId: number
  code: string
  name: string
  deptPath?: string
  sortOrder?: number
  children?: DepartmentNode[]
}

export function listUsers(params: UserQuery) {
  return get<PageResult<SystemUser>>('/system/users', params)
}

export function getUser(id: number) {
  return get<SystemUser>(`/system/users/${id}`)
}

export function createUser(data: UserCreateRequest) {
  return post<number>('/system/users', data)
}

export function updateUser(id: number, data: UserUpdateRequest) {
  return put<void>(`/system/users/${id}`, data)
}

export function deleteUser(id: number) {
  return del<void>(`/system/users/${id}`)
}

export function lockUser(id: number) {
  return put<void>(`/system/users/${id}/lock`)
}

export function unlockUser(id: number) {
  return put<void>(`/system/users/${id}/unlock`)
}

export function resetPassword(id: number, newPassword: string) {
  return put<void>(`/system/users/${id}/reset-password`, { newPassword })
}

export function assignUserRoles(id: number, roleIds: number[]) {
  return put<void>(`/system/users/${id}/roles`, { roleIds })
}

export function listRoles() {
  return get<Role[]>('/system/roles')
}

export function assignRolePermissions(id: number, permissionIds: number[]) {
  return put<void>(`/system/roles/${id}/permissions`, { permissionIds })
}

export function listPermissionTree() {
  return get<PermissionNode[]>('/system/permissions/tree')
}

export function listDepartments() {
  return get<DepartmentNode[]>('/system/departments')
}
