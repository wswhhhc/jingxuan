import request from '../request'

export interface RoleItem {
  id: string
  roleName: string
  roleCode: string
  description: string
  createTime: string
}

export async function getRoles(params: { page?: number; size?: number; excludeSystem?: boolean }) {
  const res = await request.get('/api/v1/roles', { params })
  const page = res.data as { items?: RoleItem[]; pageInfo?: { total?: number } }
  res.data = { records: page.items ?? [], total: page.pageInfo?.total ?? 0 }
  return res
}

export function createRole(data: Partial<RoleItem>) {
  return request.post('/api/v1/roles', data)
}

export function updateRole(id: string, data: Partial<RoleItem>) {
  return request.put(`/api/v1/roles/${id}`, data)
}

export function deleteRole(id: string) {
  return request.delete(`/api/v1/roles/${id}`)
}

export function getRoleMenus(id: string) {
  return request.get<string[]>(`/api/v1/roles/${id}/menus`)
}

export function updateRoleMenus(id: string, menuIds: string[]) {
  return request.put(`/api/v1/roles/${id}/menus`, menuIds)
}
