import { beforeEach, describe, expect, it, vi } from 'vitest'
import ScorePage from '../index.vue'
import { mountView } from '../../../__tests__/test-utils'

const {
  routeMock,
  replaceMock,
  getScoredWorkListMock,
  getWorkDetailMock,
  submitScoreMock,
  getMyScoreMock,
  getBatchListMock,
  successMock,
} = vi.hoisted(() => ({
  routeMock: { path: '/teacher/score', query: {} as Record<string, any> },
  replaceMock: vi.fn(),
  getScoredWorkListMock: vi.fn(),
  getWorkDetailMock: vi.fn(),
  submitScoreMock: vi.fn(),
  getMyScoreMock: vi.fn(),
  getBatchListMock: vi.fn(),
  successMock: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => routeMock,
  useRouter: () => ({ replace: replaceMock }),
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: successMock,
    },
    ElMessageBox: {
      confirm: vi.fn().mockResolvedValue(true),
    },
  }
})

vi.mock('@/api/teacher/work', () => ({
  getScoredWorkList: getScoredWorkListMock,
  getWorkDetail: getWorkDetailMock,
}))

vi.mock('@/api/teacher/score', () => ({
  submitScore: submitScoreMock,
  getMyScore: getMyScoreMock,
  getBatchList: getBatchListMock,
}))

describe('Teacher score view', () => {
  beforeEach(() => {
    routeMock.query = {}
    replaceMock.mockReset()
    getScoredWorkListMock.mockReset()
    getWorkDetailMock.mockReset()
    submitScoreMock.mockReset()
    getMyScoreMock.mockReset()
    getBatchListMock.mockReset()
    successMock.mockReset()
    getBatchListMock.mockResolvedValue({ data: [{ id: 1, batchName: '2026春' }] })
    getScoredWorkListMock.mockResolvedValue({
      data: {
        records: [{ id: 11, title: '评审作品', techStack: 'Vue', batchId: 1, scored: false }],
        total: 1,
      },
    })
    getWorkDetailMock.mockResolvedValue({
      data: {
        id: 11,
        title: '评审作品',
        summary: '摘要',
        techStack: 'Vue,TS',
        runDesc: 'npm install',
        attachments: [{ id: 1, fileName: 'demo.zip', fileUrl: 'https://cdn/demo.zip' }],
      },
    })
    getMyScoreMock.mockResolvedValue({ data: null })
  })

  it('loads review list and batches on mount', async () => {
    const wrapper = await mountView(ScorePage)

    expect(getBatchListMock).toHaveBeenCalled()
    expect(getScoredWorkListMock).toHaveBeenCalledWith({
      page: 1,
      size: 5,
      batchId: undefined,
      keyword: '',
      techStack: '',
      onlyUnscored: false,
    })
    expect(wrapper.text()).toContain('作品评分列表')
  })

  it('selects a work and fills detail + score state', async () => {
    const wrapper = await mountView(ScorePage)

    await (wrapper.vm as any).selectWork({ id: 11, title: '评审作品', techStack: 'Vue', batchId: 1 })

    expect(getWorkDetailMock).toHaveBeenCalledWith(11)
    expect(getMyScoreMock).toHaveBeenCalledWith(11)
    expect((wrapper.vm as any).selectedWork.title).toBe('评审作品')
  })

  it('submits score and marks current work as scored', async () => {
    submitScoreMock.mockResolvedValue({})
    const wrapper = await mountView(ScorePage)
    ;(wrapper.vm as any).selectedWork = { id: 11 }
    ;(wrapper.vm as any).selectedWorkScored = false

    await (wrapper.vm as any).handleSubmit()

    expect(submitScoreMock).toHaveBeenCalledWith(
      expect.objectContaining({ workId: 11, innovation: 15, difficulty: 15, completion: 20, practicality: 12 }),
    )
    expect(successMock).toHaveBeenCalledWith('评分提交成功')
    expect((wrapper.vm as any).selectedWorkScored).toBe(true)
  })
})
