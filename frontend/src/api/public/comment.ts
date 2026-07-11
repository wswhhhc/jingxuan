import request from '../request'

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

export function getCommentList(workId: string | number, pageNum = 1, pageSize = 10) {
  return request.get(`/comment/list/${workId}`, { params: { pageNum, pageSize } })
}

export function addComment(workId: string | number, content: string, parentId?: string | number, guestName?: string) {
  return request.post('/comment/add', null, { params: { workId, content, parentId, guestName } })
}

export function deleteComment(commentId: string | number) {
  return request.delete(`/comment/${commentId}`)
}
