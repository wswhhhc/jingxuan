import request from '../request'
import type { WorkListVO, WorkDetailVO } from '@/api/types'

export interface AuditQuery {
  page?: number
  size?: number
  status?: number
  keyword?: string
  techStack?: string
  classId?: number
  batchId?: number
  submitTimeBegin?: string
  submitTimeEnd?: string
}

export interface AuditAction {
  workId: number
  result: 'approved' | 'rejected'
  reason?: string
}

export interface AuditHistoryItem {
  id: number
  workId: number
  workTitle: string
  auditorName: string
  result: number
  resultLabel: string
  reason: string
  auditTime: string
}

export function getAuditList(params: AuditQuery) {
  return request.get<{ records: WorkListVO[]; total: number }>('/api/admin/audit/list', { params })
}

export function getAuditDetail(id: number) {
  return request.get<WorkDetailVO>(`/api/admin/audit/${id}`)
}

export function doAudit(data: AuditAction) {
  return request.post('/api/admin/audit', data)
}

export function getAuditHistory(workId: number) {
  return request.get<{ records: AuditHistoryItem[]; total: number }>(`/api/admin/audit/${workId}/history`)
}

export function publishWork(workId: number) {
  return request.post(`/api/admin/audit/${workId}/publish`)
}

export function offlineWork(workId: number) {
  return request.post(`/api/admin/audit/${workId}/offline`)
}

export function setFeatured(workId: number, featured: 0 | 1, previewUrl?: string) {
  return request.post(`/api/admin/audit/${workId}/featured`, null, {
    params: { featured, previewUrl: previewUrl || undefined },
  })
}
