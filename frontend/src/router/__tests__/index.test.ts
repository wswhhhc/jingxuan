import { beforeEach, describe, expect, it } from 'vitest'
import { resolveAuthRedirect } from '../index'

type TestRoute = Parameters<typeof resolveAuthRedirect>[0]

function createRoute(options?: Partial<TestRoute>): TestRoute {
  return {
    meta: {},
    matched: [],
    ...options,
  }
}

describe('router auth guard', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('allows public routes without authentication', () => {
    const redirect = resolveAuthRedirect(
      createRoute({
        meta: { noAuth: true },
      }),
    )

    expect(redirect).toBeUndefined()
  })

  it('redirects unauthenticated users to /login for protected routes', () => {
    const redirect = resolveAuthRedirect(
      createRoute({
        matched: [{ meta: { roles: ['student'] } }],
      }),
    )

    expect(redirect).toBe('/login')
  })

  it('redirects student users away from admin routes', () => {
    sessionStorage.setItem('token', 'token')
    localStorage.setItem('userInfo', JSON.stringify({ roleCode: 'ROLE_STUDENT' }))

    const redirect = resolveAuthRedirect(
      createRoute({
        matched: [{ meta: { roles: ['admin'] } }],
      }),
    )

    expect(redirect).toBe('/login')
  })

  it('redirects teacher users away from student routes', () => {
    localStorage.setItem('token', 'token')
    localStorage.setItem('userInfo', JSON.stringify({ roleCode: 'ROLE_TEACHER' }))

    const redirect = resolveAuthRedirect(
      createRoute({
        matched: [{ meta: { roles: ['student'] } }],
      }),
    )

    expect(redirect).toBe('/login')
  })

  it('allows matching role with valid token', () => {
    localStorage.setItem('token', 'token')
    localStorage.setItem('userInfo', JSON.stringify({ roleCode: 'ROLE_ADMIN' }))

    const redirect = resolveAuthRedirect(
      createRoute({
        matched: [{ meta: { roles: ['admin'] } }],
      }),
    )

    expect(redirect).toBeUndefined()
  })

  it('redirects to /login when userInfo is missing for role-protected routes', () => {
    localStorage.setItem('token', 'token')

    const redirect = resolveAuthRedirect(
      createRoute({
        matched: [{ meta: { roles: ['teacher'] } }],
      }),
    )

    expect(redirect).toBe('/login')
  })

  it('redirects to /login when userInfo is malformed', () => {
    localStorage.setItem('token', 'token')
    localStorage.setItem('userInfo', '{bad json')

    const redirect = resolveAuthRedirect(
      createRoute({
        matched: [{ meta: { roles: ['teacher'] } }],
      }),
    )

    expect(redirect).toBe('/login')
  })
})
