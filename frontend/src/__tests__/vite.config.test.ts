import { describe, expect, it } from 'vitest'
import config from '../../vite.config'

describe('Vite 开发代理', () => {
  it('剥离公共接口的 /api 前缀，使其匹配后端匿名接口', () => {
    const proxy = config.server?.proxy as Record<string, { rewrite?: (path: string) => string }>

    expect(proxy['/api/public'].rewrite?.('/api/public/ranking/batches')).toBe('/public/ranking/batches')
  })
})
