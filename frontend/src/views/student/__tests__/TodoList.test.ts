import { beforeEach, describe, expect, it, vi } from 'vitest'
import TodoList from '../TodoList.vue'
import { mountView } from '../../__tests__/test-utils'

const { getMyTasksMock } = vi.hoisted(() => ({
  getMyTasksMock: vi.fn(),
}))

vi.mock('@/api/v1/task', () => ({
  getMyTasks: getMyTasksMock,
}))

describe('Student TodoList view', () => {
  beforeEach(() => {
    getMyTasksMock.mockReset()
    getMyTasksMock.mockResolvedValue({
      data: [
        { id: 1, title: '提交作品', status: 0, batchName: '2026春', createdAt: '2026-06-01T00:00:00+08:00' },
      ],
    })
  })

  it('loads todos on mount', async () => {
    const wrapper = await mountView(TodoList)
    expect(getMyTasksMock).toHaveBeenCalled()
    expect(wrapper.text()).toContain('我的待办')
  })
})
