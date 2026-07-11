import request from '../request'

export interface AdminCommentItem {
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

export interface CommentWorkOption {
  workId: number
  workTitle: string
}

export function getAdminCommentList(params: {
  page?: number
  size?: number
  workId?: number
  userKeyword?: string
  contentKeyword?: string
}) {
  return request.get('/admin/comment/list', { params })
}

export function getAdminCommentWorkOptions() {
  return request.get<CommentWorkOption[]>('/admin/comment/work-options')
}

export function deleteAdminComment(commentId: number) {
  return request.delete(`/admin/comment/${commentId}`)
}
