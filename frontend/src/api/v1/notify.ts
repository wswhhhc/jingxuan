import request from '../request'

export interface NotifyItem {
  id: number
  title: string
  content: string
  isRead: number
  createTime: string
}

export function getNotifyList(params: { page?: number; size?: number; unreadOnly?: boolean }) {
  return request.get<{ records: NotifyItem[]; total: number }>('/api/v1/me/notifications', { params })
}

export function markAsRead(id: number) {
  return request.post(`/api/v1/me/notifications/${id}/read`)
}

export function markAllRead() {
  return request.post('/api/v1/me/notifications/read-all')
}

export function getUnreadCount() {
  return request.get<{ count: number }>('/api/v1/me/notifications/unread-count')
}

export function deleteRead() {
  return request.delete('/api/v1/me/notifications/read')
}

export function createNotifyApi() {
  return {
    getNotifyList: (params: { page?: number; size?: number; unreadOnly?: boolean }) => getNotifyList(params),
    markAsRead: (id: number) => markAsRead(id),
    markAllRead: () => markAllRead(),
    deleteRead: () => deleteRead(),
  }
}
