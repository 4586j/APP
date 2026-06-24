/** 登录请求 */
export interface LoginRequest {
  username: string
  password: string
  captcha?: string
}

/** 登录响应 — 对应 erp-security 的 LoginResponse */
export interface LoginResponse {
  token: string
  refreshToken?: string
  tokenType: 'Bearer'
  expiresIn: number
  userInfo: UserInfo
}

/** 用户信息 */
export interface UserInfo {
  id: number
  username: string
  realName: string
  email?: string
  phone?: string
  avatar?: string
  department: Department
  departmentName?: string
  roles: string[]
  permissions: string[]
}

/** 部门枚举 — 5 个部门 */
export type Department =
  | 'SALES'
  | 'PURCHASE'
  | 'FINANCE'
  | 'LOGISTICS'
  | 'MANAGEMENT'

/** 修改密码请求 */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}
