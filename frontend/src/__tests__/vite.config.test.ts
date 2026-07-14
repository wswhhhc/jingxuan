import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import config from '../../vite.config'

describe('Vite 开发代理', () => {
  it('保留公共接口的 /api 前缀，与生产 Nginx 和后端路由一致', () => {
    const proxy = config.server?.proxy as Record<string, { rewrite?: (path: string) => string }>

    expect(proxy['/api/public']).toBeUndefined()
    expect(proxy['/api'].rewrite).toBeUndefined()
  })

  it('Docker Nginx 保留 /api 前缀，与开发代理一致', () => {
    const nginxConfig = readFileSync(join(process.cwd(), 'nginx.conf'), 'utf8')

    expect(nginxConfig).not.toMatch(/rewrite\s+\^\/api\//)
    expect(nginxConfig).toContain('proxy_pass http://backend:8080;')
  })
})
