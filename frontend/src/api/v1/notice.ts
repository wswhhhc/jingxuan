import request from '../request'

export interface NoticeItem {
  id: number
  title: string
  content: string
  publisherId?: number
  publisherName?: string
  publishTime: string
  status: number
  topFlag?: number
  targetScope?: string
}

export function getNoticeList(params: { page?: number; size?: number; keyword?: string; status?: number }) {
  return request.get('/api/v1/notices', { params })
}

export function getNoticeDetail(id: number) {
  return request.get(`/api/v1/notices/${id}`)
}

export function createNotice(data: {
  title: string
  content: string
  publishDirectly?: boolean
  targetScope?: string
}) {
  return request.post('/api/v1/notices', data)
}

export function updateNotice(
  id: number,
  data: { title?: string; content?: string; targetScope?: string },
) {
  return request.put(`/api/v1/notices/${id}`, data)
}

export function deleteNotice(id: number) {
  return request.delete(`/api/v1/notices/${id}`)
}
