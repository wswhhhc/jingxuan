import request from '../request'
import type { V1Page } from './admin'

export interface V1ScoreBatchItem {
  id: number
  batchName: string
  startTime: string
  endTime: string
  classScopes: string
  status: number
  rankPublished: number
  createTime: string
  noticeTitle?: string
  noticeContent?: string
}

// 管理员端

export function getBatchList(page: number, pageSize: number) {
  return request.get<V1Page<V1ScoreBatchItem>>('/api/v1/batches', { params: { page, pageSize } })
}

export function createBatch(data: Record<string, unknown>) {
  return request.post('/api/v1/batches', data)
}

export function updateBatch(id: number, data: Record<string, unknown>) {
  return request.put(`/api/v1/batches/${id}`, data)
}

export function deleteBatch(id: number) {
  return request.delete(`/api/v1/batches/${id}`)
}

export function getActiveBatch() {
  return request.get('/api/v1/batches/active')
}

export interface V1TeacherScoreItem {
  teacherName: string
  innovation: number
  difficulty: number
  completion: number
  practicality: number
  total: number
  comment: string
}

export interface V1BatchScoreDetail {
  workId: number
  workTitle: string
  submitterName: string
  scores: V1TeacherScoreItem[]
}

export function getBatchScoreDetail(batchId: number) {
  return request.get<V1BatchScoreDetail[]>(`/api/v1/scores/batch/${batchId}`)
}

// 教师端

export function getTeacherBatches() {
  return request.get<V1ScoreBatchItem[]>('/api/v1/batches/teacher')
}

// ===== 批次额外操作 =====

export function saveNotice(batchId: number, data: { title: string; content: string }) {
  return request.put(`/api/v1/batches/${batchId}/notice`, data)
}

export function publishTasks(batchId: number) {
  return request.post(`/api/v1/batches/${batchId}/tasks/publish`)
}

export function publishBatchRanking(batchId: number) {
  return request.post(`/api/v1/batches/${batchId}/ranking/publish`)
}

export function unpublishBatchRanking(batchId: number) {
  return request.post(`/api/v1/batches/${batchId}/ranking/unpublish`)
}
