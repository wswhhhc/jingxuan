import { beforeEach, describe, expect, it, vi } from 'vitest'
import Ranking from '../Ranking.vue'
import { mountView } from '../../__tests__/test-utils'

const { getRankingListMock, getRankingBatchesMock, getRankingCategoriesMock } = vi.hoisted(() => ({
  getRankingListMock: vi.fn(),
  getRankingBatchesMock: vi.fn(),
  getRankingCategoriesMock: vi.fn(),
}))

vi.mock('@/api/public/ranking', () => ({
  getRankingList: getRankingListMock,
  getRankingBatches: getRankingBatchesMock,
  getRankingCategories: getRankingCategoriesMock,
}))

describe('Public Ranking view', () => {
  beforeEach(() => {
    getRankingListMock.mockReset()
    getRankingBatchesMock.mockReset()
    getRankingCategoriesMock.mockReset()
    getRankingListMock.mockResolvedValue({
      data: [
        {
          rankNo: 1,
          workId: 1,
          workTitle: '作品A',
          techStack: 'Vue',
          avgScore: 95,
          avgInnovation: 24,
          avgDifficulty: 24,
          avgCompletion: 28,
          avgPracticality: 19,
          rewardLevel: '一等奖',
          prizeName: '证书',
        },
      ],
    })
    getRankingBatchesMock.mockResolvedValue({ data: [{ batchId: 1, batchName: '2026春' }] })
    getRankingCategoriesMock.mockResolvedValue({ data: ['Vue'] })
  })

  it('loads batches, categories, and ranking list', async () => {
    const wrapper = await mountView(Ranking)

    expect(getRankingBatchesMock).toHaveBeenCalled()
    expect(getRankingCategoriesMock).toHaveBeenCalled()
    expect(getRankingListMock).toHaveBeenCalledWith({ batchId: undefined, topN: 50, techStack: undefined })
    expect(wrapper.text()).toContain('作品排行榜')
  })

  it('reloads list when query filters change', async () => {
    const wrapper = await mountView(Ranking)

    ;(wrapper.vm as any).query.batchId = 1
    ;(wrapper.vm as any).query.category = 'Vue'
    await (wrapper.vm as any).loadList()

    expect(getRankingListMock).toHaveBeenLastCalledWith({ batchId: 1, topN: 50, techStack: 'Vue' })
  })
})
