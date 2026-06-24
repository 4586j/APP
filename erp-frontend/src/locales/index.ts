import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN'
import enUS from './en-US'
import { storage, StorageKey } from '@/utils/storage'

export type Locale = 'zh-CN' | 'en-US'

export const SUPPORTED_LOCALES: Locale[] = ['zh-CN', 'en-US']
export const DEFAULT_LOCALE: Locale = 'zh-CN'

function detectLocale(): Locale {
  const saved = storage.get<Locale>(StorageKey.LOCALE)
  if (saved && SUPPORTED_LOCALES.includes(saved)) return saved
  const navLang = (typeof navigator !== 'undefined' && navigator.language) || ''
  if (navLang.toLowerCase().startsWith('zh')) return 'zh-CN'
  if (navLang.toLowerCase().startsWith('en')) return 'en-US'
  return DEFAULT_LOCALE
}

const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: detectLocale(),
  fallbackLocale: DEFAULT_LOCALE,
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
})

export function setLocale(locale: Locale) {
  if (!SUPPORTED_LOCALES.includes(locale)) return
  i18n.global.locale.value = locale
  storage.set(StorageKey.LOCALE, locale)
  document.documentElement.lang = locale
}

export default i18n
