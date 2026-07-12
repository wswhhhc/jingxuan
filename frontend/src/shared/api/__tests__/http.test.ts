import axios, { AxiosError } from 'axios'
import { afterEach, describe, expect, it } from 'vitest'
import { apiRequest } from '../http'

type ApiRequestOptions = NonNullable<Parameters<typeof apiRequest>[1]>

const supportedOptions = {
  headers: { 'X-Request-Source': 'http-contract-test' },
  params: { page: 2 },
  signal: new AbortController().signal,
} satisfies ApiRequestOptions

const optionsWithUrlOverride: ApiRequestOptions = { url: 'https://attacker.invalid/collect' }
const optionsWithMethodOverride: ApiRequestOptions = { method: 'DELETE' }
const optionsWithBaseUrlOverride: ApiRequestOptions = { baseURL: 'https://attacker.invalid' }

void [supportedOptions, optionsWithUrlOverride, optionsWithMethodOverride, optionsWithBaseUrlOverride]

describe('apiRequest', () => {
  afterEach(() => {
    sessionStorage.clear()
    localStorage.clear()
  })

  it('returns raw v2 DTOs with same-origin URLs and the access token', async () => {
    let resolvedUrl = ''
    let authorization: unknown
    const user = {
      id: '1912345678901234567',
      username: 'student',
    }
    sessionStorage.setItem('token', 'v2-access-token')

    const result = await apiRequest<typeof user>({
      url: '/api/v1/auth/me',
      method: 'GET',
      adapter: async (config) => {
        resolvedUrl = axios.getUri(config)
        authorization = config.headers.get('Authorization')
        return {
          config,
          data: user,
          headers: {},
          status: 200,
          statusText: 'OK',
        }
      },
    })

    expect(result).toEqual(user)
    expect(resolvedUrl).toBe('/api/v1/auth/me')
    expect(authorization).toBe('Bearer v2-access-token')
  })

  it('preserves supported Orval options without changing request identity', async () => {
    const controller = new AbortController()
    let resolvedUrl = ''
    let method = ''
    let requestSource: unknown
    let signal: unknown

    await apiRequest<{ ok: true }>(
      {
        url: '/api/v1/auth/me',
        method: 'GET',
        adapter: async (config) => {
          resolvedUrl = axios.getUri(config)
          method = config.method ?? ''
          requestSource = config.headers.get('X-Request-Source')
          signal = config.signal ?? undefined
          return {
            config,
            data: { ok: true },
            headers: {},
            status: 200,
            statusText: 'OK',
          }
        },
      },
      {
        headers: { 'X-Request-Source': 'http-contract-test' },
        params: { page: 2 },
        signal: controller.signal,
      },
    )

    expect(resolvedUrl).toBe('/api/v1/auth/me?page=2')
    expect(method).toBe('get')
    expect(requestSource).toBe('http-contract-test')
    expect(signal).toBe(controller.signal)
  })

  it.each([
    'https://attacker.invalid/collect',
    'http://attacker.invalid/collect',
    '//attacker.invalid/collect',
    'data:text/plain,secret',
  ])('rejects absolute or protocol-relative URL %s before dispatching the Bearer token', async (url) => {
    sessionStorage.setItem('token', 'must-not-leak')
    let dispatched = false
    let authorization: unknown

    const promise = apiRequest<never>({
      url,
      method: 'GET',
      adapter: async (config) => {
        dispatched = true
        authorization = config.headers.get('Authorization')
        throw new Error('unsafe request reached the adapter')
      },
    })

    await expect(promise).rejects.toThrow('仅允许请求 /api/v1 或其子路径')
    expect(dispatched).toBe(false)
    expect(authorization).toBeUndefined()
  })

  it.each(['/api/v10/auth/me', '/api/v1evil', '/auth/me', 'api/v1/auth/me', '/api/v1?scope=me'])(
    'rejects URL outside the v1 API boundary: %s',
    async (url) => {
      let dispatched = false

      const promise = apiRequest<never>({
        url,
        method: 'GET',
        adapter: async () => {
          dispatched = true
          throw new Error('out-of-boundary request reached the adapter')
        },
      })

      await expect(promise).rejects.toThrow('仅允许请求 /api/v1 或其子路径')
      expect(dispatched).toBe(false)
    },
  )

  it.each([
    ['url', { url: 'https://attacker.invalid/collect' }],
    ['method', { method: 'DELETE' }],
    ['baseURL', { baseURL: 'https://attacker.invalid' }],
  ])('rejects a runtime %s override before the request can dispatch', async (_key, unsafeOptions) => {
    sessionStorage.setItem('token', 'must-not-leak')
    let dispatched = false
    let resolvedUrl = ''
    let authorization: unknown

    const promise = apiRequest<never>(
      {
        url: '/api/v1/auth/me',
        method: 'GET',
        adapter: async (config) => {
          dispatched = true
          resolvedUrl = axios.getUri(config)
          authorization = config.headers.get('Authorization')
          throw new Error('unsafe options reached the adapter')
        },
      },
      unsafeOptions as unknown as ApiRequestOptions,
    )

    await expect(promise).rejects.toThrow('请求选项不得覆盖 url、method 或 baseURL')
    expect(dispatched).toBe(false)
    expect(resolvedUrl).toBe('')
    expect(authorization).toBeUndefined()
  })

  it('allows the exact /api/v1 root path', async () => {
    let resolvedUrl = ''

    await apiRequest<void>({
      url: '/api/v1',
      method: 'GET',
      adapter: async (config) => {
        resolvedUrl = axios.getUri(config)
        return {
          config,
          data: undefined,
          headers: {},
          status: 204,
          statusText: 'No Content',
        }
      },
    })

    expect(resolvedUrl).toBe('/api/v1')
  })

  it('extracts RFC Problem Details errors', async () => {
    const problem = {
      type: 'https://api.jingxuan.local/problems/validation_error',
      title: '参数校验失败',
      status: 422,
      detail: '请求参数校验失败',
      instance: '/api/v1/me/works',
      code: 'VALIDATION_ERROR',
      requestId: 'request-123',
      fieldErrors: {
        title: '不能为空',
      },
    }

    const promise = apiRequest<never>({
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

    await expect(promise).rejects.toMatchObject({
      name: 'ApiProblemError',
      message: '请求参数校验失败',
      status: 422,
      code: 'VALIDATION_ERROR',
      requestId: 'request-123',
      fieldErrors: {
        title: '不能为空',
      },
    })
  })
})
