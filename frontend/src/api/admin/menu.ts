import request from '../request'

export interface MenuItem {
  id: number
  menuName: string
  parentId: number
  path: string
  permission: string
  type: number
  icon: string
  sort: number
  children?: MenuItem[]
}

export function getMenuTree() {
  return request.get<MenuItem[]>('/admin/menus/tree')
}

export function createMenu(data: Partial<MenuItem>) {
  return request.post('/admin/menus', data)
}

export function updateMenu(id: number, data: Partial<MenuItem>) {
  return request.put(`/admin/menus/${id}`, data)
}

export function deleteMenu(id: number) {
  return request.delete(`/admin/menus/${id}`)
}
