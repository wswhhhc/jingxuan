import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import Dashboard from '../index.vue'
import { elementStubs, mountView } from '../../../__tests__/test-utils'

const { getTeacherDashboardStatsMock } = vi.hoisted(() => ({
  getTeacherDashboardStatsMock: vi.fn(),
}))

vi.mock('@/api/teacher/dashboard', () => ({
  getTeacherDashboardStats: getTeacherDashboardStatsMock,
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))

const ElProgressStub = defineComponent({
  name: 'ElProgress',
  props: { percentage: { type: Number, required: true } },
  template: '<div />',
})

describe('教师工作台', () => {
  beforeEach(() => {
    getTeacherDashboardStatsMock.mockResolvedValue({
      data: {
        pendingWorks: 1,
        scoredWorks: 1,
        totalScorableWorks: 2,
        completionRate: '50',
        activeBatchCount: 1,
        unreadCount: 0,
      },
    })
  })

  it('将接口返回的字符串完成率转换为进度条所需的数值', async () => {
    const wrapper = await mountView(Dashboard, {
      global: {
        stubs: { ...elementStubs, 'el-progress': ElProgressStub },
        directives: { loading: () => undefined },
      },
    })

    expect(wrapper.findComponent(ElProgressStub).props('percentage')).toBe(50)
  })
})
