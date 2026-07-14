import { describe, expect, it } from 'vitest'
import { unwrapApiResponse } from '../http'

describe('unwrapApiResponse', () => {
  it('unwraps the compatibility envelope returned by the shared Axios client', () => {
    const payload = { accessToken: 'token', username: 'admin' }

    expect(unwrapApiResponse({ code: 0, data: payload })).toBe(payload)
  })

  it('keeps a raw v1 payload unchanged', () => {
    const payload = { id: '1', roleCode: 'ADMIN' }

    expect(unwrapApiResponse(payload)).toBe(payload)
  })
})
