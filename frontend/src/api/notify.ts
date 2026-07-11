import request from './request'

export interface NotifyItem {
  id: number
  title: string
  content: string
  isRead: number
  createTime: string
}

export function getNotifyList(role: string, params: { page?: number; size?: number; unreadOnly?: boolean }) {
  return request.get<{ records: NotifyItem[]; total: number }>(`/${role}/notify/list`, { params })
}

export function markAsRead(role: string, id: number) {
  return request.post(`/${role}/notify/read/${id}`)
}

export function markAllRead(role: string) {
  return request.post(`/${role}/notify/read-all`)
}

export function getUnreadCount(role: string) {
  return request.get<{ count: number }>(`/${role}/notify/unread-count`)
}

export function deleteRead(role: string) {
  return request.delete(`/${role}/notify/read`)
}

/**
 * 为指定角色创建预绑定的通知 API 对象，
 * 方便作为 prop 传给 NotificationList 组件使用。
 */
export function createNotifyApi(role: string) {
  return {
    getNotifyList: (params: { page?: number; size?: number; unreadOnly?: boolean }) => getNotifyList(role, params),
    markAsRead: (id: number) => markAsRead(role, id),
    markAllRead: () => markAllRead(role),
    deleteRead: () => deleteRead(role),
  }
}
