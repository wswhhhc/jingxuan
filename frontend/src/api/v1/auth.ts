import request from '../request'

// ===== 类型定义 =====

export interface LoginForm {
  username: string
  password: string
  remember?: boolean
}

export interface UserInfo {
  id: string
  username: string
  realName: string
  roleName: string
  portalType?: string
  avatar?: string
  classId?: number
  className?: string
  [key: string]: unknown
}

// ===== 认证接口 =====

export function login(data: LoginForm) {
  return request.post('/api/v1/auth/login', {
    username: data.username,
    password: data.password,
    rememberMe: data.remember,
  })
}

export function getUserInfo() {
  return request.get('/api/v1/auth/me')
}

export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request.put('/api/v1/auth/password', data)
}

export function updateProfile(data: Partial<UserInfo>) {
  return request.put('/api/v1/auth/profile', data)
}