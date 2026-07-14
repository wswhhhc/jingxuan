import { beforeEach, describe, expect, it, vi } from 'vitest'
import Register from '../Register.vue'
import { mountView } from './test-utils'

const { requestGet, requestPost, request } = vi.hoisted(() => ({
  requestGet: vi.fn(),
  requestPost: vi.fn(),
  request: vi.fn(),
}))

vi.mock('@/api/request', () => ({
  default: Object.assign(request, { get: requestGet, post: requestPost }),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}))

describe('注册页', () => {
  beforeEach(() => {
    request.mockReset()
    requestGet.mockReset()
    requestPost.mockReset()
    requestGet.mockResolvedValue({ data: [] })
  })

  it('从公开的 V1 班级接口加载学生班级', async () => {
    await mountView(Register)

    expect(requestGet).toHaveBeenCalledWith('/api/v1/classes')
  })

  it('从 API 认证端点发送邮箱验证码', async () => {
    const wrapper = await mountView(Register)
    ;(wrapper.vm as any).form.email = 'student@example.com'
    ;(wrapper.vm as any).form.roleId = 1

    await wrapper.findAll('button')[0].trigger('click')

    expect(requestPost).toHaveBeenCalledWith('/api/auth/send-code', {
      email: 'student@example.com',
      roleId: 1,
    })
  })
})
