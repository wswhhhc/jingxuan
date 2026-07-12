import request from '../request'
import type { WorkListVO, WorkDetailVO } from '@/api/types'

export interface WorkQuery {
  page?: number
  size?: number
  keyword?: string
  techStack?: string
  classId?: number
  batchId?: number
  onlyUnscored?: boolean
}

export function getScoredWorkList(params: WorkQuery) {
  return request.get<{ records: WorkListVO[]; total: number }>('/api/teacher/work/list', { params })
}

export function getWorkDetail(id: number) {
  return request.get<WorkDetailVO>(`/api/teacher/work/${id}`)
}
