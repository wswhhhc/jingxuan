import request from '../request'

export interface MenuItem {
  id: string
  menuName: string
  parentId: string
  path: string
  permission: string
  type: string
  icon: string
  sort: number
  children?: MenuItem[]
}

export function getMenuTree() {
  return request.get<MenuItem[]>('/api/v1/menus/tree')
}
