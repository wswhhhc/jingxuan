import request from '../request'

export interface RoleItem {
  id: number
  roleName: string
  roleCode: string
  description: string
  createTime: string
}

export function getRoles(params: { page?: number; size?: number; excludeSystem?: boolean }) {
  return request.get('/admin/roles', { params })
}

export function getRoleDetail(id: number) {
  return request.get(`/admin/roles/${id}`)
}

export function createRole(data: Partial<RoleItem>) {
  return request.post('/admin/roles', data)
}

export function updateRole(id: number, data: Partial<RoleItem>) {
  return request.put(`/admin/roles/${id}`, data)
}

export function deleteRole(id: number) {
  return request.delete(`/admin/roles/${id}`)
}

export function getRoleMenus(id: number) {
  return request.get<number[]>(`/admin/roles/${id}/menus`)
}

export function updateRoleMenus(id: number, menuIds: number[]) {
  return request.put(`/admin/roles/${id}/menus`, menuIds)
}
