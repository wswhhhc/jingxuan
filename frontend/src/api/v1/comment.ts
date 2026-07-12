import request from '../request'
import type { V1Page } from './admin'

export interface CommentItem {
  id: string | number
  workId: string | number
  userId: string | number | null
  guestName?: string
  content: string
  parentId: string | number | null
  replyToUserName?: string
  createTime: string
  userName: string
  avatarUrl?: string
  roleName: string
  replies: CommentItem[]
}

export async function getCommentList(workId: string | number, pageNum = 1, pageSize = 10) {
  const res = await request.get(`/api/v1/works/${workId}/comments`, {
    params: { page: pageNum, size: pageSize },
  })
  // 转换 v1 { items, pageInfo } 为旧版 { records, total }
  if (res.data) {
    const v1 = res.data as V1Page<CommentItem>
    res.data = {
      records: v1.items ?? [],
      total: v1.pageInfo?.total ?? 0,
    }
  }
  return res
}

export function addComment(workId: string | number, content: string, parentId?: string | number, guestName?: string) {
  const body: Record<string, unknown> = { content }
  if (parentId !== undefined) body.parentId = parentId
  if (guestName !== undefined) body.guestName = guestName
  return request.post(`/api/v1/works/${workId}/comments`, body)
}

export function deleteComment(commentId: string | number) {
  return request.delete(`/api/v1/works/comments/${commentId}`)
}

// ===== 管理员评论管理 =====

export interface V1AdminCommentItem {
  id: number
  workId: number
  workTitle: string
  userId: number
  userName: string
  roleName: string
  content: string
  parentId: number | null
  replyToUserName?: string
  createTime: string
}

export function getAdminCommentList(params: {
  page?: number
  size?: number
  workId?: number
  userKeyword?: string
  contentKeyword?: string
}) {
  return request.get<V1Page<V1AdminCommentItem>>('/api/v1/works/comments', { params })
}

export function deleteAdminComment(commentId: number) {
  return request.delete(`/api/v1/works/comments/${commentId}`)
}

