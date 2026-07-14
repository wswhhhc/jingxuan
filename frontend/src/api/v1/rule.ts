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

export function getRuleList(params: { page?: number; size?: number; keyword?: string }) {
  return request.get('/api/v1/moderation/rules', { params })
}

export function createRule(data: Partial<RuleItem>) {
  return request.post('/api/v1/moderation/rules', data)
}

export function updateRule(id: number, data: Partial<RuleItem>) {
  return request.put(`/api/v1/moderation/rules/${id}`, data)
}

export function deleteRule(id: number) {
  return request.delete(`/api/v1/moderation/rules/${id}`)
}

/** v1 无此功能，临时保留旧实现 */
export function testConnection() {
  return request.post('/api/admin/rule/test-connection')
}
