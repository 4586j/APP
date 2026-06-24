import { get, post } from './request'
import type {
  LoginRequest,
  LoginResponse,
  UserInfo,
  ChangePasswordRequest,
  CaptchaResponse,
  RefreshTokenRequest,
} from '@/types/auth'

/** GET /api/v1/auth/captcha — 颁发验证码 */
export function getCaptcha() {
  return get<CaptchaResponse>('/auth/captcha')
}

/** POST /api/v1/auth/login — 登录获取 access + refresh token */
export function login(data: LoginRequest) {
  return post<LoginResponse>('/auth/login', data)
}

/** POST /api/v1/auth/logout — 拉黑当前 access token */
export function logout() {
  return post<void>('/auth/logout')
}

/** GET /api/v1/auth/me — 拉取当前用户信息（含权限码） */
export function getCurrentUser() {
  return get<UserInfo>('/auth/me')
}

/** POST /api/v1/auth/change-password — 修改密码（成功后旧 access 立即失效） */
export function changePassword(data: ChangePasswordRequest) {
  return post<void>('/auth/change-password', data)
}

/** POST /api/v1/auth/refresh — 用 refresh token 换新的 access+refresh */
export function refreshToken(data: RefreshTokenRequest) {
  return post<LoginResponse>('/auth/refresh', data)
}
