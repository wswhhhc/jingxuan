import { beforeEach, describe, expect, it, vi } from 'vitest'
import Register from '../Register.vue'
import { mountView } from './test-utils'

const { requestGet, request } = vi.hoisted(() => ({
  requestGet: vi.fn(),
  request: vi.fn(),
}))

vi.mock('@/api/request', () => ({
  default: Object.assign(request, { get: requestGet }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))

describe('注册页', () => {
  beforeEach(() => {
    request.mockReset()
    requestGet.mockReset()
    requestGet.mockResolvedValue({ data: [] })
  })

  it('从公开的 V1 班级接口加载学生班级', async () => {
    await mountView(Register)

    expect(requestGet).toHaveBeenCalledWith('/api/v1/classes')
  })
})
