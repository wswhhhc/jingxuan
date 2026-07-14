import request from '../request'

export interface UserItem {
  id: string
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

export async function getUsers(params: { page?: number; size?: number; keyword?: string; roleId?: number; status?: number }) {
  const res = await request.get('/api/v1/users', { params })
  const page = res.data as { items?: UserItem[]; pageInfo?: { total?: number } }
  res.data = { records: page.items ?? [], total: page.pageInfo?.total ?? 0 }
  return res
}

export function createUser(data: {
  username: string
  realName: string
  roleId: number
  classId?: number
  phone?: string
  email?: string
}) {
  return request.post('/api/v1/users', data)
}

export function updateUser(
  id: string,
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
  return request.put(`/api/v1/users/${id}`, data)
}

export function updateStatus(id: string, status: number) {
  return request.put(`/api/v1/users/${id}/status`, { status })
}

export function deleteUser(id: string) {
  return request.delete(`/api/v1/users/${id}`)
}

export async function getRoles() {
  const res = await request.get('/api/v1/roles', { params: { page: 1, size: 100 } })
  const page = res.data as { items?: RoleItem[] }
  res.data = (page.items ?? []).map((role) => ({ ...role, id: Number(role.id) }))
  return res
}

export async function getClasses() {
  const res = await request.get('/api/v1/classes')
  const classes = res.data as { id?: string; label?: string }[]
  res.data = classes.map((item) => ({ id: Number(item.id), className: item.label ?? '' }))
  return res
}

export function batchImportUsers(users: Record<string, unknown>[]) {
  return request.post('/api/v1/users/batch', users)
}

export function parseAiImportUsers(messages: AiImportMessage[]) {
  return request.post('/api/v1/users/batch/ai-parse', { messages })
}
