import request from '../request'
import {
  adaptPageResult,
  adaptWorkVO,
  toBackendStatus,
  type BackendWorkVO,
} from '../workAdapter'
import type { V1Page } from './admin'
import type { V1CreateWorkRequest as V1CreateWorkRequestType } from '@/shared/api/generated/models/v1CreateWorkRequest'

// 兼容导出 WorkForm / WorkAttachment 等类型
export interface WorkItem {
  id: number
  title: string
  summary?: string
  techStack?: string | string[]
  coverUrl?: string
  submitterName?: string
  status: number
  statusLabel?: string
  featured?: number
  likeCount?: number
  viewCount?: number
  batchId?: number
  attachments?: WorkAttachment[]
  members?: Array<{ id?: number; studentName?: string; studentNo?: string; isLeader?: boolean; studentId?: number; className?: string; avatar?: string; [key: string]: unknown }>
  tags?: string[]
  submitTime?: string
  previewUrl?: string
  [key: string]: unknown
}

export interface WorkAttachment {
  id: number
  fileName: string
  fileType: string
  fileUrl: string
  fileSize: number
  [key: string]: unknown
}

export interface WorkForm {
  title: string
  summary?: string
  techStack: string | string[]
  advisor?: string
  coverUrl?: string
  videoUrl?: string
  previewUrl?: string
  runDescription?: string
  attachments: WorkAttachment[]
  members: Array<{ id?: number; studentName: string; studentNo: string; isLeader?: boolean; studentId?: number; className?: string; avatar?: string; workId?: number | string; [key: string]: unknown }>
  batchId?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNum?: number
  pageSize?: number
}
// 复用生成的 V1CreateWorkRequest 类型
export type { V1CreateWorkRequest } from '@/shared/api/generated/models/v1CreateWorkRequest'

export interface V1WorkListParams {
  page?: number
  pageSize?: number
  status?: string
  keyword?: string
}

/** 创建作品 */
export function createWork(data: V1CreateWorkRequestType) {
  return request.post('/api/v1/me/works', data)
}

/** 更新作品 */
export function updateWork(id: string | number, data: V1CreateWorkRequestType) {
  return request.put(`/api/v1/me/works/${id}`, data)
}

/** 删除作品 */
export function deleteWork(id: string | number) {
  return request.delete(`/api/v1/me/works/${id}`)
}

/** 提交作品审核 */
export function submitWork(id: string | number) {
  return request.post(`/api/v1/me/works/${id}/submissions`)
}

/** 获取我的作品列表 */
export async function getMyWorks(params: V1WorkListParams) {
  const queryParams: Record<string, string | number> = {
    page: params.page || 1,
    size: params.pageSize || 10,
  }
  const statusNum = toBackendStatus(params.status)
  if (statusNum !== undefined) queryParams.status = statusNum
  if (params.keyword) queryParams.keyword = params.keyword

  const res = await request({
    url: '/api/v1/me/works',
    method: 'get',
    params: queryParams,
  })
  // 适配后端 v1 响应为旧版 { records, total }
  const v1 = res.data as V1Page<BackendWorkVO>
  res.data = adaptPageResult(
    {
      records: (v1?.items || []) as BackendWorkVO[],
      total: v1?.pageInfo?.total || 0,
    },
    adaptWorkVO,
  )
  return res
}

/** 获取作品详情 */
export async function getWorkDetail(id: string | number) {
  const res = await request({
    url: `/api/v1/me/works/${id}`,
    method: 'get',
  })
  res.data = adaptWorkVO(res.data as BackendWorkVO)
  return res
}

/** 申请删除作品 */
export function submitDeleteRequest(workId: string | number, reason: string) {
  return request.post(`/api/v1/me/works/${workId}/deletion-requests`, { reason })
}

/** 文件上传（保留旧接口） */
export function uploadFile(file: File, workId?: string | number) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/file/upload',
    method: 'post',
    data: formData,
    params: {
      workId: workId || undefined,
    },
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
