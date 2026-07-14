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
  return request.get<MenuItem[]>('/api/admin/menus/tree')
}
