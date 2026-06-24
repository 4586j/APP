/**
 * localStorage 包装 — 带前缀避免污染，自动 JSON 序列化
 */
const PREFIX = 'erp:'

export const storage = {
  get<T = any>(key: string, defaultValue: T | null = null): T | null {
    try {
      const raw = localStorage.getItem(PREFIX + key)
      if (raw === null) return defaultValue
      return JSON.parse(raw) as T
    } catch {
      return defaultValue
    }
  },

  set(key: string, value: any): void {
    try {
      localStorage.setItem(PREFIX + key, JSON.stringify(value))
    } catch (e) {
      console.warn('[storage] set failed:', key, e)
    }
  },

  remove(key: string): void {
    localStorage.removeItem(PREFIX + key)
  },

  clear(): void {
    const keys = Object.keys(localStorage).filter((k) => k.startsWith(PREFIX))
    keys.forEach((k) => localStorage.removeItem(k))
  },
}

/** 三个固定 key — 避免散落字符串字面量 */
export const StorageKey = {
  TOKEN: 'token',
  REFRESH_TOKEN: 'refreshToken',
  USER_INFO: 'userInfo',
  LOCALE: 'locale',
} as const
