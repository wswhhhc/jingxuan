import { AxiosError } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { postAuthLogin } from '@/shared/api/generated/auth/auth'
import request, { normalizeResponsePayload } from '../request'

const { messageErrorMock } = vi.hoisted(() => ({
  messageErrorMock: vi.fn(),
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: messageErrorMock,
  },
}))

beforeEach(() => {
  messageErrorMock.mockReset()
})

describe('normalizeResponsePayload', () => {
  it('wraps a successful raw v1 payload in the legacy response envelope', () => {
    const payload = { items: [{ id: '1', title: '作品' }], pageInfo: { total: 1 } }

    expect(normalizeResponsePayload(payload)).toEqual({
      code: 0,
      data: payload,
    })
  })

  it('keeps an existing legacy response envelope unchanged', () => {
    const payload = { code: 0, data: { id: '1' } }

    expect(normalizeResponsePayload(payload)).toBe(payload)
  })
})

describe('request error handling', () => {
  it('uses RFC Problem Details detail as the rejected error message', async () => {
    const problem = {
      type: 'about:blank',
      title: '参数校验失败',
      status: 422,
      detail: '请求参数校验失败',
      instance: '/api/v1/me/works',
      code: 'VALIDATION_ERROR',
      requestId: 'request-123',
      fieldErrors: { title: '不能为空' },
    }

    const response = request({
      url: '/api/v1/me/works',
      method: 'POST',
      adapter: async (config) => {
        throw new AxiosError('Request failed with status code 422', AxiosError.ERR_BAD_REQUEST, config, undefined, {
          config,
          data: problem,
          headers: { 'content-type': 'application/problem+json' },
          status: 422,
          statusText: 'Unprocessable Entity',
        })
      },
    })

    await expect(response).rejects.toThrow('请求参数校验失败')
    expect(messageErrorMock).toHaveBeenCalledWith('请求参数校验失败')
  })

  it('preserves RFC Problem Details fields through the generated client', async () => {
    const problem = {
      type: 'https://api.jingxuan.local/problems/validation_error',
      title: '参数校验失败',
      status: 422,
      detail: '请求参数校验失败',
      instance: '/api/v1/auth/login',
      code: 'VALIDATION_ERROR',
      requestId: 'request-456',
      fieldErrors: { username: '不能为空' },
    }

    const response = postAuthLogin(
      { username: '', password: '' },
      {
        adapter: async (config) => {
          throw new AxiosError('Request failed with status code 422', AxiosError.ERR_BAD_REQUEST, config, undefined, {
            config,
            data: problem,
            headers: { 'content-type': 'application/problem+json' },
            status: 422,
            statusText: 'Unprocessable Entity',
          })
        },
      },
    )

    await expect(response).rejects.toMatchObject({
      name: 'ApiProblemError',
      message: problem.detail,
      ...problem,
    })
  })

  it('keeps the legacy message ahead of a detail fallback', async () => {
    const response = request({
      url: '/api/admin/audit',
      method: 'POST',
      adapter: async (config) => {
        throw new AxiosError('Request failed with status code 400', AxiosError.ERR_BAD_REQUEST, config, undefined, {
          config,
          data: {
            code: 400,
            message: '旧接口错误消息',
            detail: '不应覆盖旧接口消息',
          },
          headers: { 'content-type': 'application/json' },
          status: 400,
          statusText: 'Bad Request',
        })
      },
    })

    await expect(response).rejects.toThrow('旧接口错误消息')
    expect(messageErrorMock).toHaveBeenCalledWith('旧接口错误消息')
  })

  it.each([[{ title: 42 }], [['不应接受数组']]])(
    'falls back to a plain Error for invalid Problem Details fieldErrors %#',
    async (fieldErrors) => {
      const response = request({
        url: '/api/v1/me/works',
        method: 'POST',
        adapter: async (config) => {
          throw new AxiosError('Request failed with status code 422', AxiosError.ERR_BAD_REQUEST, config, undefined, {
            config,
            data: {
              type: 'https://api.jingxuan.local/problems/validation_error',
              title: '参数校验失败',
              status: 422,
              detail: '请求参数校验失败',
              instance: '/api/v1/me/works',
              code: 'VALIDATION_ERROR',
              requestId: 'request-invalid',
              fieldErrors,
            },
            headers: { 'content-type': 'application/problem+json' },
            status: 422,
            statusText: 'Unprocessable Entity',
          })
        },
      })

      await expect(response).rejects.toMatchObject({
        name: 'Error',
        message: '请求参数校验失败',
      })
    },
  )
})
