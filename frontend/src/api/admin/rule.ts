import request from '../request'

export interface RuleItem {
  id: number
  ruleName: string
  systemPrompt: string
  enabledCategories: string
  onRejectAction: string
  createTime: string
  status: number
}

export function getRuleList(params: { page?: number; size?: number }) {
  return request.get('/api/admin/rule/list', { params })
}

export function createRule(data: Partial<RuleItem>) {
  return request.post('/api/admin/rule', data)
}

export function updateRule(id: number, data: Partial<RuleItem>) {
  return request.put(`/api/admin/rule/${id}`, data)
}

export function deleteRule(id: number) {
  return request.delete(`/api/admin/rule/${id}`)
}

export function testConnection() {
  return request.post('/api/admin/rule/test-connection')
}
