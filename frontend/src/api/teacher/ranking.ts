import request from '../request'
import type { RankItem, CategoryItem } from '../types'

export function getRanking(params: { batchId?: number; type?: string; topN?: number }) {
  return request.get<RankItem[]>('/teacher/ranking/list', { params })
}

export function getRankingBatches() {
  return request.get('/teacher/ranking/batches')
}

export function getRankingCategories(batchId?: number) {
  return request.get<CategoryItem[]>('/teacher/ranking/categories', { params: { batchId } })
}

export function refreshRanking(batchId: number) {
  return request.post(`/teacher/ranking/refresh/${batchId}`)
}
