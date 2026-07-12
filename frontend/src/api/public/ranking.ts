import request from '../request'
import type { RankItem } from '../types'

export function getRankingList(params: { batchId?: number; topN?: number; techStack?: string }) {
  return request.get<RankItem[]>('/api/public/ranking/list', { params })
}

export function getRankingBatches() {
  return request.get('/api/public/ranking/batches')
}

export function getRankingCategories(batchId?: number) {
  return request.get<string[]>('/api/public/ranking/categories', { params: { batchId } })
}
