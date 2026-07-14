import request from '../request'
import type { UserInfo } from '../types'

export type { UserInfo }

export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request({
    url: '/api/auth/password',
    method: 'put',
    data,
  })
}

export function updateProfile(data: Partial<UserInfo>) {
  return request({
    url: '/api/auth/profile',
    method: 'put',
    data,
  })
}
