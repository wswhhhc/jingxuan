import request from '../request'
import type { V1Page } from './admin'

// ===== 类型定义 =====

export interface V1PrizeItem {
  id: number
  batchId: number
  batchName: string
  rewardLevel: string
  rewardName: string
  prizeName: string
  quota: number
}

export interface V1IssueItem {
  id: number
  rewardId: number
  workId: number
  issueStatus: number
  issueTime: string
  operatorId: number
  remark?: string
  rewardName?: string
  workTitle?: string
}

export interface V1RankedWork {
  rankNo: number
  workId: number
  workTitle: string
  techStack: string
  avgScore: number
}

// ===== 奖项管理 =====

export function getPrizeList(params: { page?: number; size?: number; batchId?: number }) {
  return request.get<V1Page<V1PrizeItem>>('/api/v1/prizes', { params })
}

export function createPrize(data: Partial<V1PrizeItem>) {
  return request.post('/api/v1/prizes', data)
}

export function updatePrize(id: number, data: Partial<V1PrizeItem>) {
  return request.put(`/api/v1/prizes/${id}`, data)
}

export function deletePrize(id: number) {
  return request.delete(`/api/v1/prizes/${id}`)
}

/** 获取所有批次（用于奖项筛选下拉） */
export function getPrizeBatches() {
  return request.get('/api/v1/batches')
}

// ===== 发放追踪 =====

export function getIssueList(params: { page?: number; size?: number; rewardId?: number }) {
  if (params.rewardId) {
    return request.get<V1Page<V1IssueItem>>(`/api/v1/prizes/${params.rewardId}/issues`, {
      params: { page: params.page, size: params.size },
    })
  }
  // 无 rewardId 时查询全部：遍历所有奖项的问题（不支持分页全部查询，使用空列表降级）
  return Promise.resolve({ data: { items: [], pageInfo: { page: 1, pageSize: 10, total: 0, totalPages: 0 } } })
}

export function issuePrize(data: { rewardId: number; workId: number }) {
  return request.post(`/api/v1/prizes/${data.rewardId}/issues`, { workId: data.workId })
}

export function cancelIssue(issueId: number) {
  return request.put(`/api/v1/prizes/issues/${issueId}/cancel`)
}

export function getRankedWorks(params: { batchId: number; topN?: number }) {
  return request.get<V1RankedWork[]>('/api/v1/prizes/ranked-works', { params })
}
