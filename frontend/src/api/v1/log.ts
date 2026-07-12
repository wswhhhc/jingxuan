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

export async function getLogList(params: { page: number; size: number; action?: string; userId?: number }) {
  const res = await request.get('/api/v1/audit-logs', { params })
  // 转换 v1 { items, pageInfo } 为旧版 { records, total }
  if (res.data) {
    res.data = {
      records: res.data.items ?? [],
      total: res.data.pageInfo?.total ?? 0,
    }
  }
  return res
}
