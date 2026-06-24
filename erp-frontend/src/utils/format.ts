import dayjs from 'dayjs'

/** 日期格式化（YYYY-MM-DD HH:mm:ss） */
export function formatDateTime(d?: string | Date | number | null): string {
  if (!d) return '-'
  const m = dayjs(d)
  return m.isValid() ? m.format('YYYY-MM-DD HH:mm:ss') : '-'
}

/** 日期格式化（YYYY-MM-DD） */
export function formatDate(d?: string | Date | number | null): string {
  if (!d) return '-'
  const m = dayjs(d)
  return m.isValid() ? m.format('YYYY-MM-DD') : '-'
}

/** 金额格式化（千分位 + 2 位小数 + 货币符号） */
export function formatMoney(
  v?: number | string | null,
  currency = 'CNY',
  digits = 2,
): string {
  if (v == null || v === '') return '-'
  const n = typeof v === 'string' ? Number(v) : v
  if (Number.isNaN(n)) return '-'
  const symbol: Record<string, string> = { CNY: '¥', USD: '$', EUR: '€', JPY: '¥' }
  const prefix = symbol[currency] ?? ''
  return prefix + n.toFixed(digits).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/** 文件大小格式化 */
export function formatBytes(bytes?: number | null): string {
  if (bytes == null || bytes < 0) return '-'
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return (bytes / Math.pow(1024, i)).toFixed(2) + ' ' + units[i]
}

/** 截断 + 省略号 */
export function truncate(s?: string | null, max = 30): string {
  if (!s) return '-'
  return s.length <= max ? s : s.slice(0, max) + '…'
}
