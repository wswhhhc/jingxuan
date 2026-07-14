import { beforeEach, describe, expect, it, vi } from 'vitest'
import MyRanking from '../MyRanking.vue'
import { mountView } from '../../__tests__/test-utils'

const { requestGet } = vi.hoisted(() => ({ requestGet: vi.fn() }))

vi.mock('@/api/request', () => ({
  default: { get: requestGet },
}))

describe('学生我的评分页', () => {
  beforeEach(() => {
    requestGet.mockReset()
    requestGet.mockResolvedValue({ data: [] })
  })

  it('通过带 api 前缀的学生排名接口加载数据', async () => {
    await mountView(MyRanking)

    expect(requestGet).toHaveBeenCalledWith('/api/student/score/my-ranks')
  })
})
