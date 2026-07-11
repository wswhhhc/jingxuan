import request from '../request'

export interface PrizeItem {
  id: number
  batchId: number
  batchName: string
  rewardLevel: string
  rewardName: string
  prizeName: string
  quota: number
}

export function getPrizeList(params: { page?: number; size?: number; batchId?: number }) {
  return request.get('/admin/prize/list', { params })
}

export function createPrize(data: Partial<PrizeItem>) {
  return request.post('/admin/prize', data)
}

export function updatePrize(id: number, data: Partial<PrizeItem>) {
  return request.put(`/admin/prize/${id}`, data)
}

export function deletePrize(id: number) {
  return request.delete(`/admin/prize/${id}`)
}

export function getPrizeBatches() {
  return request.get('/admin/prize/batches')
}

export interface IssueItem {
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

export function getIssueList(params: { page?: number; size?: number; rewardId?: number }) {
  return request.get('/admin/prize/issue/list', { params })
}

export function issuePrize(data: { rewardId: number; workId: number; operatorId: number }) {
  return request.post('/admin/prize/issue', data)
}

export function cancelIssue(id: number) {
  return request.put(`/admin/prize/issue/${id}/cancel`)
}

export interface RankedWork {
  rankNo: number
  workId: number
  workTitle: string
  techStack: string
  avgScore: number
}

export function getRankedWorks(params: { batchId: number; topN?: number }) {
  return request.get<RankedWork[]>('/admin/prize/ranked-works', { params })
}
