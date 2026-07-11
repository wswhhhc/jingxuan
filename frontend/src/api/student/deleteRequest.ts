import request from '../request'

export function submitDeleteRequest(workId: number | string, reason: string) {
  return request.post(`/student/work/${workId}/delete-request`, { reason })
}
