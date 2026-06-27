import { get, post, put, del } from './request'

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages?: number
}

export type Id = string | number

export interface SystemUser {
  id: Id
  username: string
  realName: string
  email?: string
  phone?: string
  avatarUrl?: string
  departmentId?: Id
  departmentName?: string
  superiorId?: Id
  status: number
  pwdResetRequired?: number
  lastLoginTime?: string
  lastLoginIp?: string
  lockedUntil?: string
  createdAt?: string
  roleCodes?: string[]
  permCodes?: string[]
  roleIds?: Id[]
}

export interface UserQuery {
  page?: number
  size?: number
  username?: string
  realName?: string
  departmentId?: Id
  status?: number
  roleId?: Id
  excludeRoleId?: Id
}

export interface UserCreateRequest {
  username: string
  password: string
  realName: string
  email?: string
  phone?: string
  departmentId?: Id
  superiorId?: Id
  roleIds?: Id[]
  status?: number
}

export interface UserUpdateRequest {
  realName?: string
  email?: string
  phone?: string
  avatarUrl?: string
  departmentId?: Id
  superiorId?: Id
  status?: number
}

export interface Role {
  id: Id
  roleName: string
  roleCode: string
  dataScope?: number
  description?: string
  status?: number
  createdAt?: string
  permissionIds?: Id[]
}

export interface PermissionNode {
  id: Id
  parentId: Id
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
  id: Id
  parentId: Id
  code: string
  name: string
  deptPath?: string
  sortOrder?: number
  status?: number
  createdAt?: string
  children?: DepartmentNode[]
}

export function listUsers(params: UserQuery) {
  return get<PageResult<SystemUser>>('/system/users', params)
}

export function getUser(id: Id) {
  return get<SystemUser>(`/system/users/${id}`)
}

export function createUser(data: UserCreateRequest) {
  return post<Id>('/system/users', data)
}

export function updateUser(id: Id, data: UserUpdateRequest) {
  return put<void>(`/system/users/${id}`, data)
}

export function deleteUser(id: Id) {
  return del<void>(`/system/users/${id}`)
}

export function lockUser(id: Id) {
  return put<void>(`/system/users/${id}/lock`)
}

export function unlockUser(id: Id) {
  return put<void>(`/system/users/${id}/unlock`)
}

export function resetPassword(id: Id, newPassword: string) {
  return put<void>(`/system/users/${id}/reset-password`, { newPassword })
}

export function batchResetPassword(userIds: Id[], newPassword: string) {
  return post<void>('/system/users/batch-reset-password', { userIds, newPassword })
}

export function assignUserRoles(id: Id, roleIds: Id[]) {
  return put<void>(`/system/users/${id}/roles`, { roleIds })
}

export interface BatchImportResult {
  successCount: number
  failList: { index: number; username: string; reason: string }[]
}

export function batchCreateUsers(list: UserCreateRequest[]) {
  return post<BatchImportResult>('/system/users/batch', list)
}

export function importUsersExcel(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return post<BatchImportResult>('/system/users/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function downloadUserTemplate() {
  return get<Blob>('/system/users/import-template', undefined, { responseType: 'blob' })
}

export function listRoles() {
  return get<Role[]>('/system/roles')
}

export function assignRolePermissions(id: Id, permissionIds: Id[]) {
  return put<void>(`/system/roles/${id}/permissions`, { permissionIds })
}

export function listPermissionTree() {
  return get<PermissionNode[]>('/system/permissions/tree')
}

export function listDepartments() {
  return get<DepartmentNode[]>('/system/departments')
}

export function getDepartmentOptions(keyword?: string) {
  return get<DepartmentNode[]>('/system/departments/select-options', { keyword })
}

export interface DepartmentCreateRequest {
  parentId?: Id
  code: string
  name: string
  sortOrder?: number
  status?: number
}

export interface DepartmentUpdateRequest {
  parentId?: Id
  code?: string
  name?: string
  sortOrder?: number
  status?: number
}

export function createDepartment(data: DepartmentCreateRequest) {
  return post<Id>('/system/departments', data)
}

export function updateDepartment(id: Id, data: DepartmentUpdateRequest) {
  return put<void>(`/system/departments/${id}`, data)
}

export function deleteDepartment(id: Id) {
  return del<void>(`/system/departments/${id}`)
}

export interface RoleCreateRequest {
  roleName: string
  roleCode: string
  dataScope?: number
  description?: string
  status?: number
}

export interface RoleUpdateRequest {
  roleName?: string
  dataScope?: number
  description?: string
  status?: number
}

export function createRole(data: RoleCreateRequest) {
  return post<Id>('/system/roles', data)
}

export function updateRole(id: Id, data: RoleUpdateRequest) {
  return put<void>(`/system/roles/${id}`, data)
}

export function deleteRole(id: Id) {
  return del<void>(`/system/roles/${id}`)
}

export function assignRoleUsers(id: Id, userIds: Id[]) {
  return put<void>(`/system/roles/${id}/users`, { userIds })
}

// ==================== 权限管理 API ====================
export interface PermissionCreateRequest {
  parentId?: Id
  name: string
  code: string
  type?: 'menu' | 'button' | 'api'
  httpMethod?: string
  icon?: string
  path?: string
  component?: string
  sortOrder?: number
  status?: number
}

export interface PermissionUpdateRequest {
  name?: string
  sortOrder?: number
  path?: string
  icon?: string
  status?: number
}

export function listPermissions() {
  return get<PermissionNode[]>('/system/permissions')
}

export function getPermission(id: Id) {
  return get<PermissionNode>(`/system/permissions/${id}`)
}

export function createPermission(data: PermissionCreateRequest) {
  return post<Id>('/system/permissions', data)
}

export function updatePermission(id: Id, data: PermissionUpdateRequest) {
  return put<void>(`/system/permissions/${id}`, data)
}

export function deletePermission(id: Id) {
  return del<void>(`/system/permissions/${id}`)
}

// ==================== 部门权限分配 API ====================
export function getDeptPermissionIds(id: Id) {
  return get<Id[]>(`/system/departments/${id}/permissions`)
}

export function assignDeptPermissions(id: Id, permissionIds: Id[]) {
  return put<void>(`/system/departments/${id}/permissions`, { permissionIds })
}
