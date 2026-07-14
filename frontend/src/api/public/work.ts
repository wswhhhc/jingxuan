import request from '../request'
import { adaptPageResult, adaptWorkVO } from '../workAdapter'

export interface PublicWorkListParams {
  page?: number
  pageSize?: number
  keyword?: string
  techStack?: string
  classId?: number
  tagIds?: number[]
  submitTimeBegin?: string
  submitTimeEnd?: string
  sortBy?: string
}

export interface TagItem {
  id: number
  dictLabel: string
}

/* ============ API ============ */

export async function getPublicWorkList(params: PublicWorkListParams) {
  const res = await request({
    url: '/api/v1/showcase/works',
    method: 'get',
    params: {
      page: params.page || 1,
      size: params.pageSize || 12,
      keyword: params.keyword || undefined,
      techStack: params.techStack || undefined,
      classId: params.classId || undefined,
      tagIds: params.tagIds?.length ? params.tagIds.join(',') : undefined,
      submitTimeBegin: params.submitTimeBegin || undefined,
      submitTimeEnd: params.submitTimeEnd || undefined,
    },
  })
  res.data = adaptPageResult(res.data, adaptWorkVO, 12)
  return res
}

export interface PublicClassItem {
  id: number
  dictValue: string
  dictLabel: string
}

export async function getPublicClassList() {
  return request.get<PublicClassItem[]>('/api/v1/classes')
}

export async function getPublicWorkDetail(id: string | number) {
  const res = await request({
    url: `/api/v1/showcase/works/${id}`,
    method: 'get',
  })
  res.data = adaptWorkVO(res.data)
  return res
}

export async function getPublicTagList() {
  return request.get<TagItem[]>('/api/v1/tags')
}

/** 点赞/取消点赞 */
export function toggleLike(workId: number) {
  return request.put(`/api/v1/works/${workId}/likes`)
}
