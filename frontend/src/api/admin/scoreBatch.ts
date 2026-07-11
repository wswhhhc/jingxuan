import request from '../request'

export interface ScoreBatchItem {
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

export function getBatchList(pageNum: number, pageSize: number) {
  return request.post('/score-batch/list', null, { params: { pageNum, pageSize } })
}

export function createBatch(data: Partial<ScoreBatchItem>) {
  return request.post('/score-batch/create', data)
}

export function updateBatch(data: Partial<ScoreBatchItem>) {
  return request.put('/score-batch/update', data)
}

export function deleteBatch(id: number) {
  return request.delete(`/score-batch/${id}`)
}

export function getActiveBatch() {
  return request.get('/score-batch/active')
}

export interface TeacherScoreItem {
  teacherName: string
  innovation: number
  difficulty: number
  completion: number
  practicality: number
  total: number
  comment: string
}

export interface BatchScoreDetail {
  workId: number
  workTitle: string
  submitterName: string
  scores: TeacherScoreItem[]
}

export function getBatchScoreDetail(batchId: number) {
  return request.get(`/admin/score/batch/${batchId}`)
}
