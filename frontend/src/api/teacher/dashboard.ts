import request from '../request'

export interface TeacherDashboardStats {
  pendingWorks: number
  scoredWorks: number
  totalScorableWorks: number
  completionRate: number
  activeBatchCount: number
  unreadCount: number
}

export function getTeacherDashboardStats() {
  return request.get<TeacherDashboardStats>('/teacher/dashboard/stats')
}
