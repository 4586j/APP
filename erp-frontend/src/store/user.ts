import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, logout as apiLogout, getCurrentUser } from '@/api/auth'
import { storage, StorageKey } from '@/utils/storage'
import type { UserInfo, LoginRequest } from '@/types/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(storage.get<string>(StorageKey.TOKEN) || '')
  const userInfo = ref<UserInfo | null>(storage.get<UserInfo>(StorageKey.USER_INFO))
  const permissions = computed<string[]>(() => userInfo.value?.permissions ?? [])
  const roles = computed<string[]>(() => userInfo.value?.roles ?? [])

  const isLoggedIn = computed(() => !!token.value)
  const department = computed(() => userInfo.value?.department || '')

  async function login(payload: LoginRequest) {
    const resp = await apiLogin(payload)
    token.value = resp.token
    userInfo.value = resp.userInfo
    storage.set(StorageKey.TOKEN, resp.token)
    if (resp.refreshToken) storage.set(StorageKey.REFRESH_TOKEN, resp.refreshToken)
    storage.set(StorageKey.USER_INFO, resp.userInfo)
    return resp
  }

  async function fetchProfile() {
    if (!token.value) return null
    const info = await getCurrentUser()
    userInfo.value = info
    storage.set(StorageKey.USER_INFO, info)
    return info
  }

  async function logout() {
    try {
      if (token.value) await apiLogout()
    } catch {
      // 即便后端 logout 失败也清本地
    } finally {
      clear()
    }
  }

  function clear() {
    token.value = ''
    userInfo.value = null
    storage.remove(StorageKey.TOKEN)
    storage.remove(StorageKey.REFRESH_TOKEN)
    storage.remove(StorageKey.USER_INFO)
  }

  function hasPermission(perm: string): boolean {
    if (!perm) return true
    if (roles.value.includes('ADMIN')) return true
    return permissions.value.includes(perm)
  }

  function hasAnyPermission(perms: string[]): boolean {
    return perms.some((p) => hasPermission(p))
  }

  return {
    token,
    userInfo,
    permissions,
    roles,
    isLoggedIn,
    department,
    login,
    logout,
    fetchProfile,
    hasPermission,
    hasAnyPermission,
    clear,
  }
})
