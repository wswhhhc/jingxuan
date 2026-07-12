import { beforeEach, describe, expect, it, vi } from 'vitest'
import LogList from '../index.vue'
import { mountView } from '../../../__tests__/test-utils'

const { getLogListMock } = vi.hoisted(() => ({
  getLogListMock: vi.fn(),
}))

vi.mock('@/api/admin/log', () => ({
  getLogList: getLogListMock,
}))

describe.skip('Admin Log view', () => {
  beforeEach(() => {
    getLogListMock.mockReset()
    getLogListMock.mockResolvedValue({
      data: {
        records: [
          { id: 1, action: '登录', username: 'admin', success: true, createdAt: '2026-06-01T00:00:00+08:00' },
        ],
        total: 1,
      },
    })
  })

  it('loads log list on mount', async () => {
    await mountView(LogList)
    expect(getLogListMock).toHaveBeenCalled()
  })
})
