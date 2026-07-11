import request from '../request'

export interface ScoreSubmit {
  workId: number
  innovation: number
  difficulty: number
  completion: number
  practicality: number
  comment: string
}

export interface ScoreRecord {
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

export function submitScore(data: ScoreSubmit) {
  return request.post('/teacher/score', data)
}

export function getMyScore(workId: number) {
  return request.get(`/teacher/score/${workId}`)
}

export function getScoreHistory(params: { page?: number; size?: number }) {
  return request.get('/teacher/score/history', { params })
}

export function getBatchList() {
  return request.get('/teacher/batch/list')
}
