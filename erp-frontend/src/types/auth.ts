/** 登录请求 — 对齐 erp-security LoginRequest（B1.4 Phase 2） */
export interface LoginRequest {
  username: string
  password: string
  /** 可选验证码 uuid（由 /auth/captcha 颁发） */
  captchaUuid?: string
  /** 可选验证码答案（不区分大小写） */
  captchaCode?: string
}

/** 登录响应 — 对齐 erp-security LoginResponse */
export interface LoginResponse {
  accessToken: string
  refreshToken: string
  userInfo: UserInfo
}

/** 用户信息 — 对齐 erp-security UserInfo（注意 department 是字符串非枚举） */
export interface UserInfo {
  id: number
  username: string
  realName: string
  department: string
  departmentName?: string
  departmentId?: number
  roles: string[]
  permissions: string[]
  // 前端可选扩展字段（后端尚未返回）
  email?: string
  phone?: string
  avatar?: string
}

/** 修改密码请求 — 对齐 erp-security ChangePasswordRequest */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

/** 验证码响应 — 对齐 erp-security CaptchaResponse */
export interface CaptchaResponse {
  uuid: string
  /** 已带 data:image/png;base64, 前缀，可直接给 <img :src=""> */
  imageBase64: string
}

/** 刷新 token 请求 */
export interface RefreshTokenRequest {
  refreshToken: string
}
