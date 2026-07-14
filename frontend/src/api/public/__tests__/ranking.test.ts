import { beforeEach, describe, expect, it, vi } from 'vitest'

const { requestGet, request } = vi.hoisted(() => ({
  requestGet: vi.fn(),
  request: vi.fn(),
}))

vi.mock('../../request', () => ({
  default: Object.assign(request, { get: requestGet }),
}))

import { getRankingBatches, getRankingCategories, getRankingList } from '../ranking'

describe('公共排行榜 API', () => {
  beforeEach(() => {
    request.mockReset()
    requestGet.mockReset()
  })

  it('使用部署环境的 API 前缀访问公开接口', () => {
    getRankingList({ batchId: 1, topN: 20, techStack: 'Vue' })
    getRankingBatches()
    getRankingCategories(1)

    expect(requestGet).toHaveBeenNthCalledWith(1, '/api/public/ranking/list', {
      params: { batchId: 1, topN: 20, techStack: 'Vue' },
    })
    expect(requestGet).toHaveBeenNthCalledWith(2, '/api/public/ranking/batches')
    expect(requestGet).toHaveBeenNthCalledWith(3, '/api/public/ranking/categories', { params: { batchId: 1 } })
  })
})
