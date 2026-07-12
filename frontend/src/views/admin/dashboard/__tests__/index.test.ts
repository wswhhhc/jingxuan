import { beforeEach, describe, expect, it, vi } from 'vitest'
import Dashboard from '../index.vue'
import { mountView } from '../../../__tests__/test-utils'
import { createPinia } from 'pinia'

const { getDashboardStatsMock, getDashboardChartsMock } = vi.hoisted(() => ({
  getDashboardStatsMock: vi.fn(),
  getDashboardChartsMock: vi.fn(),
}))

vi.mock('@/api/admin/dashboard', () => ({
  getDashboardStats: getDashboardStatsMock,
  getDashboardCharts: getDashboardChartsMock,
}))

describe.skip('Admin Dashboard view', () => {
  beforeEach(() => {
    getDashboardStatsMock.mockReset()
    getDashboardChartsMock.mockReset()
    getDashboardStatsMock.mockResolvedValue({
      data: {
        totalWorks: 100, pendingAudit: 10, publishedWorks: 80,
        totalTeachers: 20, totalStudents: 500, activeBatches: 2,
        recentWorks: [], scoreDistribution: {},
      },
    })
    getDashboardChartsMock.mockResolvedValue({
      data: { techStackDistribution: [], statusDistribution: {}, scoreDistribution: {} },
    })
  })

  it('loads stats and charts on mount', async () => {
    await mountView(Dashboard, { global: { plugins: [createPinia()] } })
    expect(getDashboardStatsMock).toHaveBeenCalled()
    expect(getDashboardChartsMock).toHaveBeenCalled()
  })
})
