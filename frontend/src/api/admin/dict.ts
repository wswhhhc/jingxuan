import request from '../request'

export interface DictItem {
  id: number
  dictType: string
  dictLabel: string
  dictValue: string
  sort: number
  remark: string
  createTime: string
}

/** 获取所有字典（按类型分组） */
export function getAllDicts() {
  return request.get('/admin/dict/all')
}

/** 创建字典项 */
export function createDict(data: {
  dictType: string
  dictLabel: string
  dictValue: string
  sort?: number
  remark?: string
}) {
  return request.post('/admin/dict/create', data)
}

/** 更新字典项 */
export function updateDict(data: {
  id: number
  dictType?: string
  dictLabel?: string
  dictValue?: string
  sort?: number
  remark?: string
}) {
  return request.put('/admin/dict/update', data)
}

/** 删除字典项 */
export function deleteDict(id: number) {
  return request.delete(`/admin/dict/${id}`)
}
