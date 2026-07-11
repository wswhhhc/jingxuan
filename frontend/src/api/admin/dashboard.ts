import request from '../request'

export interface DashboardStats {
  totalWorks: number
  pendingAudit: number
  publishedWorks: number
  totalTeachers: number
  totalStudents: number
  activeBatches: number
  recentWorks: {
    id: number
    title: string
    status: number
    submitTime: string
    submitterId?: number
    submitterName?: string
  }[]
  scoreDistribution: { range: string; count: number }[]
}

export function getDashboardStats() {
  return request.get<DashboardStats>('/admin/dashboard/stats')
}

export interface ChartData {
  techStackDist: { name: string; value: number }[]
  statusDist: Record<string, number>
  scoreDist: { name: string; value: number }[]
}

export function getDashboardCharts() {
  return request.get<ChartData>('/admin/dashboard/charts')
}
