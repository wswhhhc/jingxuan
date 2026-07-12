import { beforeEach, describe, expect, it, vi } from 'vitest'
import NoticeList from '../index.vue'
import { mountView } from '../../../__tests__/test-utils'

const { getNoticeListMock, deleteNoticeMock } = vi.hoisted(() => ({
  getNoticeListMock: vi.fn(),
  deleteNoticeMock: vi.fn(),
}))

vi.mock('@/api/v1/notice', () => ({
  getNoticeList: getNoticeListMock,
  deleteNotice: deleteNoticeMock,
}))

describe('Admin Notice view', () => {
  beforeEach(() => {
    getNoticeListMock.mockReset()
    deleteNoticeMock.mockReset()
    getNoticeListMock.mockResolvedValue({
      data: {
        items: [
          { id: '1', title: '公告1', status: 'PUBLISHED', targetScope: 'all', createdAt: '2026-06-01T00:00:00+08:00' },
        ],
        pageInfo: { page: 1, pageSize: 20, total: 1, totalPages: 1 },
      },
    })
  })

  it('loads notice list on mount', async () => {
    const wrapper = await mountView(NoticeList)
    expect(getNoticeListMock).toHaveBeenCalled()
    expect(wrapper.text()).toContain('发布公告')
  })

  it('deletes a notice', async () => {
    const wrapper = await mountView(NoticeList)
    deleteNoticeMock.mockResolvedValue({})
    const rows = wrapper.findAll('.el-table-stub .row-trigger')
    if (rows.length > 0) {
      await rows[0].trigger('click')
    }
  })
})
