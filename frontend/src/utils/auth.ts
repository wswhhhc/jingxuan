import type { UserInfo } from '@/api/types'

export function getAuthToken() {
  return sessionStorage.getItem('token') || localStorage.getItem('token') || ''
}

export function hasLoginToken() {
  return !!getAuthToken()
}

export function getCachedUserInfo(): UserInfo | null {
  try {
    const raw = localStorage.getItem('userInfo')
    return raw ? (JSON.parse(raw) as UserInfo) : null
  } catch {
    return null
  }
}

export function clearAuthStorage() {
  sessionStorage.removeItem('token')
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
}
