import request from '../request'
import type { V1Page } from './admin'

// ===== 教师评分 =====

export interface V1ScoreSubmit {
  workId: number
  innovation: number
  difficulty: number
  completion: number
  practicality: number
  comment: string
}

export interface V1ScoreRecord {
  id: number
  workId: number
  workTitle: string
  batchId?: number | null
  innovation: number
  difficulty: number
  completion: number
  practicality: number
  total: number
  comment: string
  scoreTime: string
}

export function submitScore(data: V1ScoreSubmit) {
  return request.put(`/api/v1/works/${data.workId}/scores/me`, {
    innovation: data.innovation,
    difficulty: data.difficulty,
    completion: data.completion,
    practicality: data.practicality,
    comment: data.comment,
  })
}

/** 获取教师对某个作品的评分 */
export function getMyScore(workId: number) {
  return request.get<V1ScoreRecord>(`/api/v1/works/${workId}/scores`)
}

/** 获取评分历史 */
export function getScoreHistory(params: { page?: number; size?: number }) {
  return request.get<V1Page<V1ScoreRecord>>('/api/v1/me/scores/history', { params })
}

/** 教师端批次列表（用于评分筛选） */
export function getTeacherBatchList() {
  return request.get('/api/v1/batches')
}
