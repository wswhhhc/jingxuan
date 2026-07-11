import { beforeEach, describe, expect, it, vi } from 'vitest'
import WorkList from '../WorkList.vue'
import { mountView } from '../../__tests__/test-utils'

const { pushMock, getPublicWorkListMock, getPublicClassListMock, getPublicTagListMock } = vi.hoisted(() => ({
  pushMock: vi.fn(),
  getPublicWorkListMock: vi.fn(),
  getPublicClassListMock: vi.fn(),
  getPublicTagListMock: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}))

vi.mock('@/api/public/work', () => ({
  getPublicWorkList: getPublicWorkListMock,
  getPublicClassList: getPublicClassListMock,
  getPublicTagList: getPublicTagListMock,
}))

describe('Public WorkList view', () => {
  beforeEach(() => {
    pushMock.mockReset()
    getPublicWorkListMock.mockReset()
    getPublicClassListMock.mockReset()
    getPublicTagListMock.mockReset()
    getPublicWorkListMock.mockResolvedValue({
      data: {
        records: [
          {
            id: 1,
            title: '智慧校园',
            summary: '简介',
            submitTime: '2026-05-24 12:00:00',
            submitterName: '张三',
            featured: 1,
            tags: ['Vue'],
            likeCount: 6,
            viewCount: 12,
          },
        ],
        total: 1,
      },
    })
    getPublicClassListMock.mockResolvedValue({ data: [{ id: 1, dictLabel: '软工1班' }] })
    getPublicTagListMock.mockResolvedValue({ data: [{ id: 1, name: 'Vue' }] })
  })

  it('loads list, classes, and tags on mount', async () => {
    const wrapper = await mountView(WorkList)

    expect(getPublicClassListMock).toHaveBeenCalled()
    expect(getPublicTagListMock).toHaveBeenCalled()
    expect(getPublicWorkListMock).toHaveBeenCalledWith({
      page: 1,
      pageSize: 12,
      keyword: undefined,
      techStack: undefined,
      classId: undefined,
      submitTimeBegin: undefined,
      submitTimeEnd: undefined,
    })
    expect(wrapper.text()).toContain('学院作品展廊')
    expect(wrapper.text()).toContain('智慧校园')
  })

  it('searches with keyword and resets page', async () => {
    const wrapper = await mountView(WorkList)

    ;(wrapper.vm as any).page = 3
    ;(wrapper.vm as any).query.keyword = 'AI'
    ;(wrapper.vm as any).search()

    expect((wrapper.vm as any).page).toBe(1)
    expect(getPublicWorkListMock).toHaveBeenLastCalledWith(expect.objectContaining({ keyword: 'AI' }))
  })

  it('resets filters and clears date range', async () => {
    const wrapper = await mountView(WorkList)

    ;(wrapper.vm as any).query.keyword = 'AI'
    ;(wrapper.vm as any).query.techStack = 'Vue'
    ;(wrapper.vm as any).query.classId = 2
    ;(wrapper.vm as any).submitTimeRange = ['2026-05-01', '2026-05-02']
    ;(wrapper.vm as any).query.submitTimeBegin = '2026-05-01'
    ;(wrapper.vm as any).query.submitTimeEnd = '2026-05-02'
    ;(wrapper.vm as any).resetSearch()

    expect((wrapper.vm as any).query.keyword).toBe('')
    expect((wrapper.vm as any).query.techStack).toBe('')
    expect((wrapper.vm as any).query.classId).toBeUndefined()
    expect((wrapper.vm as any).submitTimeRange).toBeNull()
  })
})
