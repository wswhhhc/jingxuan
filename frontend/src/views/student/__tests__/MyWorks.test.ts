import { beforeEach, describe, expect, it, vi } from 'vitest'
import MyWorks from '../MyWorks.vue'
import { mountView } from '../../__tests__/test-utils'

const { pushMock, getMyWorksMock, deleteWorkMock, successMock, authStore } = vi.hoisted(() => ({
  pushMock: vi.fn(),
  getMyWorksMock: vi.fn(),
  deleteWorkMock: vi.fn(),
  successMock: vi.fn(),
  authStore: {
    userInfo: { id: 101 },
  },
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: successMock,
    },
  }
})

vi.mock('@/api/student/work', () => ({
  getMyWorks: getMyWorksMock,
  deleteWork: deleteWorkMock,
}))

vi.mock('@/app/session/auth', () => ({
  useAuthStore: () => authStore,
}))

describe('MyWorks view', () => {
  beforeEach(() => {
    pushMock.mockReset()
    getMyWorksMock.mockReset()
    deleteWorkMock.mockReset()
    successMock.mockReset()
    getMyWorksMock.mockResolvedValue({
      data: {
        records: [
          {
            id: 1,
            title: '作品A',
            status: 'draft',
            submitterId: 101,
            techStack: 'Vue',
            advisor: '张老师',
            createTime: '2026-05-25',
          },
          {
            id: 2,
            title: '作品B',
            status: 'submitted',
            submitterId: 101,
            techStack: 'Java',
            advisor: '李老师',
            createTime: '2026-05-24',
          },
          {
            id: 3,
            title: '作品C',
            status: 'approved',
            submitterId: 101,
            techStack: 'Go',
            advisor: '王老师',
            createTime: '2026-05-23',
          },
        ],
        total: 3,
      },
    })
  })

  it('loads list and computes status stats', async () => {
    const wrapper = await mountView(MyWorks)

    expect(getMyWorksMock).toHaveBeenCalledWith({ page: 1, pageSize: 10, status: undefined })
    expect(wrapper.text()).toContain('我的作品档案')
    expect((wrapper.vm as any).statusStats.submitted).toBe(1)
    expect((wrapper.vm as any).statusStats.approved).toBe(1)
  })

  it('reloads from page 1 when filter changes', async () => {
    const wrapper = await mountView(MyWorks)

    ;(wrapper.vm as any).page = 3
    ;(wrapper.vm as any).filterStatus = 'submitted'
    await (wrapper.vm as any).handleFilterChange()

    expect((wrapper.vm as any).page).toBe(1)
    expect(getMyWorksMock).toHaveBeenLastCalledWith({ page: 1, pageSize: 10, status: 'submitted' })
  })

  it('deletes a work and refreshes the list', async () => {
    deleteWorkMock.mockResolvedValue({})
    const wrapper = await mountView(MyWorks)

    await (wrapper.vm as any).handleDelete(1)

    expect(deleteWorkMock).toHaveBeenCalledWith(1)
    expect(successMock).toHaveBeenCalledWith('删除成功')
    expect(getMyWorksMock).toHaveBeenCalledTimes(2)
  })
})
