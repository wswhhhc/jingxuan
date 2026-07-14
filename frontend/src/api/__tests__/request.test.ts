import { describe, expect, it } from 'vitest'
import { normalizeResponsePayload } from '../request'

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
