import { beforeEach, describe, expect, it, vi } from 'vitest'

const { requestGet, request } = vi.hoisted(() => ({
  requestGet: vi.fn(),
  request: vi.fn(),
}))

vi.mock('../../request', () => ({
  default: Object.assign(request, { get: requestGet }),
}))

import { getPublicClassList, getPublicTagList, getPublicWorkList } from '../work'

describe('公共展廊 API 适配', () => {
  beforeEach(() => {
    request.mockReset()
    requestGet.mockReset()
  })

  it('将 v1 展廊分页响应转换为卡片列表', async () => {
    request.mockResolvedValue({
      data: {
        items: [{ id: '1', title: '作品', status: 'APPROVED', submittedAt: '2026-06-03T09:28:02+08:00' }],
        pageInfo: { page: 1, pageSize: 12, total: 1 },
      },
    })

    const response = await getPublicWorkList({ page: 1, pageSize: 12 })

    expect(response.data).toMatchObject({
      total: 1,
      records: [{ id: '1', title: '作品', status: 'approved', submitTime: '2026-06-03T09:28:02+08:00' }],
    })
  })

  it('将 v1 班级与标签字段转换为下拉框所需字段', async () => {
    requestGet
      .mockResolvedValueOnce({ data: [{ id: '1', label: '软件技术 1 班', value: 'software-1' }] })
      .mockResolvedValueOnce({ data: [{ id: '10', name: 'Vue 3' }] })

    await expect(getPublicClassList()).resolves.toEqual([
      { id: '1', dictLabel: '软件技术 1 班', dictValue: 'software-1' },
    ])
    await expect(getPublicTagList()).resolves.toEqual([{ id: '10', dictLabel: 'Vue 3' }])
  })
})
