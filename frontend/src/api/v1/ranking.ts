import request from '../request'

// ===== 排行榜（v1 leaderboard API） =====

export interface V1LeaderboardItem {
  rankNo: number
  workId: number
  workTitle: string
  techStack: string
  avgInnovation: string
  avgDifficulty: string
  avgCompletion: string
  avgPracticality: string
  avgScore: string | number
  teacherCount: number
  rewardLevel?: string
  prizeName?: string
}

export interface V1CategoryItem {
  value: string
  label: string
}

export interface V1MyRankItem {
  batchId: number
  batchName: string
  workId: number
  workTitle: string
  avgScore: number | null
  avgInnovation: number | null
  avgDifficulty: number | null
  avgCompletion: number | null
  avgPracticality: number | null
  teacherCount: number
  rankNo: number | null
}

/** 获取排行榜数据 */
export function getRankingList(params: { batchId?: number; topN?: number; type?: string }) {
  return request.get<V1LeaderboardItem[]>('/api/v1/leaderboards', { params })
}

/** 获取已公示批次列表 */
export function getRankingBatches() {
  return request.get('/api/v1/leaderboards/batches')
}

/** 获取排行榜分类选项 */
export function getRankingCategories(batchId?: number) {
  return request.get<V1CategoryItem[]>('/api/v1/leaderboards/categories', { params: { batchId } })
}

/** 手动刷新排行榜缓存 */
export function refreshLeaderboard(batchId: number) {
  return request.post('/api/v1/leaderboards/cache/refresh', null, { params: { batchId } })
}

/** 获取我的排名（学生端） */
export function getMyRanks() {
  return request.get<V1MyRankItem[]>('/api/v1/leaderboards/me')
}
