import { get, post, put } from './request'
import type { Id } from './system'

export interface NotificationVO {
  id: Id
  notificationId: Id
  title: string
  content: string
  type: string
  sourceType: string
  sourceId: Id
  isRead: number
  readAt: string
  createdAt: string
}

export interface NotificationPageVO {
  records: NotificationVO[]
  total: number
  size: number
  current: number
}

export interface NotificationQuery {
  type?: string
  isRead?: number
  page?: number
  size?: number
}

/** 我的通知列表 */
export function listMyNotifications(params?: NotificationQuery) {
  return get<NotificationPageVO>('/notifications/my', params)
}

/** 我的未读数量 */
export function getUnreadCount() {
  return get<number>('/notifications/unread-count')
}

/** 标记单条已读 */
export function markNotificationRead(id: Id) {
  return put<void>(`/notifications/${id}/read`)
}

/** 批量标记已读（ids 为空=全部已读） */
export function batchMarkNotificationRead(ids?: Id[]) {
  return put<number>('/notifications/batch-read', { ids })
}
