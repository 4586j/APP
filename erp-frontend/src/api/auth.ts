import { get, post } from './request'
import type { LoginRequest, LoginResponse, UserInfo, ChangePasswordRequest } from '@/types/auth'

/** POST /api/auth/login — 登录获取 JWT */
export function login(data: LoginRequest) {
  return post<LoginResponse>('/auth/login', data)
}

/** POST /api/auth/logout — 注销并使 token 失效（加入 Redis 黑名单） */
export function logout() {
  return post<void>('/auth/logout')
}

/** GET /api/auth/me — 拉取当前用户信息（含权限码） */
export function getCurrentUser() {
  return get<UserInfo>('/auth/me')
}

/** PUT /api/auth/password — 修改密码 */
export function changePassword(data: ChangePasswordRequest) {
  return post<void>('/auth/password', data)
}
