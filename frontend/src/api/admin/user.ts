import request from '../request'

export interface UserItem {
  id: number
  username: string
  realName: string
  roleId: number
  roleName: string
  classId?: number
  className?: string
  phone?: string
  email?: string
  status: number
  createTime: string
}

export interface RoleItem {
  id: number
  roleName: string
  roleCode: string
}

export interface ClassItem {
  id: number
  className: string
}

export interface AiImportMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface AiImportUserDraft {
  username: string
  password?: string
  realName: string
  roleId?: number
  roleName?: string
  classId?: number
  className?: string
  phone?: string
  email?: string
  status?: number
}

export interface AiImportResponse {
  assistantReply: string
  ready: boolean
  requiredFields: string[]
  optionalFields: string[]
  missingFields: string[]
  assumptions: string[]
  users: AiImportUserDraft[]
}

export function getUsers(params: { page?: number; size?: number; keyword?: string; roleId?: number; status?: number }) {
  return request.get('/admin/users', { params })
}

export function createUser(data: {
  username: string
  realName: string
  roleId: number
  classId?: number
  phone?: string
  email?: string
}) {
  return request.post('/admin/users', data)
}

export function getUserDetail(id: number) {
  return request.get(`/admin/users/${id}`)
}

export function updateUser(
  id: number,
  data: {
    username?: string
    realName?: string
    roleId?: number
    classId?: number
    phone?: string
    email?: string
    password?: string
  },
) {
  return request.put(`/admin/users/${id}`, data)
}

export function updateStatus(id: number, status: number) {
  return request.put(`/admin/users/${id}/status`, null, { params: { status } })
}

export function deleteUser(id: number) {
  return request.delete(`/admin/users/${id}`)
}

export function getRoles() {
  return request.get('/admin/roles')
}

export function getClasses() {
  return request.get('/admin/dict/classes')
}

export function batchImportUsers(users: Record<string, unknown>[]) {
  return request.post('/admin/users/batch', users)
}

export function parseAiImportUsers(messages: AiImportMessage[]) {
  return request.post('/admin/users/batch/ai-parse', { messages })
}
