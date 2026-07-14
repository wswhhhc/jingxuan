import request from '../request'
import { adaptPageResult, adaptWorkVO } from '../workAdapter'

export interface PublicWorkListParams {
  page?: number
  pageSize?: number
  keyword?: string
  techStack?: string
  classId?: string | number
  tagIds?: number[]
  submitTimeBegin?: string
  submitTimeEnd?: string
  sortBy?: string
}

export interface TagItem {
  id: string
  dictLabel: string
}

interface V1ReferenceItem {
  id: string
  label: string
  value: string
}

interface V1Tag {
  id: string
  name: string
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
  id: string
  dictValue: string
  dictLabel: string
}

export async function getPublicClassList() {
  const res = await request.get<V1ReferenceItem[]>('/api/v1/classes')
  return (res.data as V1ReferenceItem[]).map((item) => ({
    id: item.id,
    dictLabel: item.label,
    dictValue: item.value,
  }))
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
  const res = await request.get<V1Tag[]>('/api/v1/tags')
  return (res.data as V1Tag[]).map((item) => ({ id: item.id, dictLabel: item.name }))
}

/** 点赞/取消点赞 */
export function toggleLike(workId: string | number) {
  return request.put(`/api/v1/works/${workId}/likes`)
}
