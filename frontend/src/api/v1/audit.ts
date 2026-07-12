import request from '../request'
import type { V1Page } from './admin'
import type { BackendWorkVO } from '../workAdapter'

// ===== 类型定义 =====

export interface V1AuditQuery {
  page?: number
  size?: number
  status?: number
  keyword?: string
  classId?: number
  submitTimeBegin?: string
  submitTimeEnd?: string
}

export interface V1AuditHistoryItem {
  id: number
  workId: number
  workTitle: string
  auditorName: string
  result: number
  resultLabel: string
  reason: string
  auditTime: string
}

// ===== 删除申请管理 =====

export interface DeleteRequestItem {
  id: number
  workId?: number
  workTitle: string
  studentName: string
  reason: string
  status: number
  adminReply?: string
}

/** 删除作品（管理员） */
export function adminDeleteWork(workId: number) {
  return request.delete(`/api/v1/works/${workId}`)
}

/** 删除申请列表 */
export function getDeleteRequests(params: { page: number; size: number; status?: number }) {
  return request.get<{ records: DeleteRequestItem[]; total: number }>('/api/v1/works/deletion-requests', { params })
}

/** 同意删除申请 */
export function approveDeleteRequest(id: number) {
  return request.post(`/api/v1/works/deletion-requests/${id}/approve`)
}

/** 拒绝删除申请 */
export function rejectDeleteRequest(id: number, reply: string) {
  return request.post(`/api/v1/works/deletion-requests/${id}/reject`, { reply })
}

export function getAuditList(params: V1AuditQuery) {
  return request.get<V1Page<BackendWorkVO>>('/api/v1/works/audit-queue', { params })
}

export function getAuditDetail(id: number) {
  return request.get<BackendWorkVO>(`/api/v1/works/${id}/audit-queue`)
}

export function doAudit(data: { workId: number; result: 'APPROVED' | 'REJECTED'; reason?: string }) {
  return request.post(`/api/v1/works/${data.workId}/audit-decisions`, {
    result: data.result,
    reason: data.reason,
  })
}

export function getAuditHistory(workId: number, params?: { page?: number; size?: number }) {
  return request.get<V1Page<V1AuditHistoryItem>>(`/api/v1/works/${workId}/audit-history`, { params })
}

export function publishWork(workId: number) {
  return request.post(`/api/v1/works/${workId}/publication`)
}

export function offlineWork(workId: number) {
  return request.post(`/api/v1/works/${workId}/publication/offline`)
}

export function setFeatured(workId: number, featured: 0 | 1, previewUrl?: string) {
  return request.post(`/api/v1/works/${workId}/publication/featured`, null, {
    params: { featured, previewUrl: previewUrl || undefined },
  })
}
