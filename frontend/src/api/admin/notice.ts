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
  return request.get('/api/admin/notice/list', { params })
}

export function getNoticeDetail(id: number) {
  return request.get(`/admin/notice/${id}`)
}

export function createNotice(data: {
  title: string
  content: string
  status?: number
  topFlag?: number
  targetScope?: string
}) {
  return request.post('/api/admin/notice', data)
}

export function updateNotice(
  id: number,
  data: { title?: string; content?: string; status?: number; topFlag?: number; targetScope?: string },
) {
  return request.put(`/admin/notice/${id}`, data)
}

export function deleteNotice(id: number) {
  return request.delete(`/admin/notice/${id}`)
}
