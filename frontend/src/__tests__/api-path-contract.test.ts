import { readFileSync, readdirSync } from 'node:fs'
import { extname, join, relative } from 'node:path'
import { describe, expect, it } from 'vitest'

const sourceRoot = join(process.cwd(), 'src')
const requestMethodPattern =
  /request\.(?:get|post|put|delete|patch)(?:<[^>]*>)?\s*\(\s*(['"`])(?<path>\/[^'"`]*)\1/gs
const requestConfigPattern =
  /\brequest\s*\(\s*\{[\s\S]*?\burl\s*:\s*(['"`])(?<path>\/[^'"`]*)\1/g

function sourceFiles(directory: string): string[] {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) {
      return entry.name === '__tests__' ? [] : sourceFiles(path)
    }
    if (!['.ts', '.vue'].includes(extname(entry.name)) || entry.name.endsWith('.test.ts')) {
      return []
    }
    return [path]
  })
}

function invalidApiPaths() {
  const invalid: string[] = []
  for (const file of sourceFiles(sourceRoot)) {
    const source = readFileSync(file, 'utf8')
    for (const pattern of [requestMethodPattern, requestConfigPattern]) {
      pattern.lastIndex = 0
      for (const match of source.matchAll(pattern)) {
        const path = match.groups?.path
        if (!path || path === '/api' || path.startsWith('/api/') || path.startsWith('/uploads/')) {
          continue
        }
        const line = source.slice(0, match.index).split('\n').length
        invalid.push(`${relative(sourceRoot, file)}:${line} -> ${path}`)
      }
    }
  }
  return invalid
}

describe('前端 API 路径契约', () => {
  it('所有后端请求都使用可被代理的 /api 或 /uploads 前缀', () => {
    expect(invalidApiPaths()).toEqual([])
  })
})
