import request from '../request'

export interface LogItem {
  id: number
  userId: number
  username: string
  action: string
  target: string
  targetId: number
  ip: string
  requestMethod: string
  requestPath: string
  duration: number
  result: number
  errorMsg: string
  createTime: string
}

export function getLogList(params: { page: number; size: number; action?: string }) {
  return request.get('/api/admin/log/list', { params })
}
