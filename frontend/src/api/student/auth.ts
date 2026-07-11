import request from '../request'
import type { UserInfo } from '../types'

export type { UserInfo }

export interface LoginForm {
  username: string
  password: string
  remember?: boolean
}

export function login(data: LoginForm) {
  return request.post('/auth/login', {
    username: data.username,
    password: data.password,
    rememberMe: data.remember,
  })
}

export function getUserInfo() {
  return request({
    url: '/auth/user-info',
    method: 'get',
  })
}

export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request({
    url: '/auth/password',
    method: 'put',
    data,
  })
}

export function updateProfile(data: Partial<UserInfo>) {
  return request({
    url: '/auth/profile',
    method: 'put',
    data,
  })
}
