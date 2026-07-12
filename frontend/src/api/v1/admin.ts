import request from '../request'

// ===== 类型定义（供视图使用） =====

/** v1 用户 DTO */
export interface V1User {
  id: string
  username: string
  realName: string
  roleId: number
  roleName: string
  classId?: number | null
  className?: string
  phone?: string
  email?: string
  status: number
  avatar?: string
  firstLogin?: boolean
  createTime?: string
  updateTime?: string
}

/** v1 角色 DTO */
export interface V1Role {
  id: string
  roleName: string
  roleCode: string
  description?: string
  createTime?: string
  updateTime?: string
}

/** v1 菜单 DTO */
export interface V1Menu {
  id: string
  menuName: string
  parentId: string
  path?: string
  permission?: string
  type?: string
  icon?: string
  sort?: number
  children?: V1Menu[]
}

/** v1 分页响应 */
export interface V1Page<T> {
  items: T[]
  pageInfo: {
    page: number
    pageSize: number
    total: number
    totalPages: number
  }
}

/** 批量导入结果 */
export interface V1BatchImportResult {
  success: number
  failed: number
  errors: string[]
}

/** AI 导入消息 */
export interface AiImportMessage {
  role: string
  content: string
}

/** AI 导入响应 */
export interface AiImportResponse {
  assistantReply: string
  ready: boolean
  requiredFields: string[]
  optionalFields: string[]
  missingFields: string[]
  assumptions: string[]
  userCount: number
}

/** AI 导入用户草稿 */
export interface AiImportUserDraft {
  username: string
  realName: string
  roleId?: number
  roleName?: string
  classId?: number
  className?: string
  phone?: string
  email?: string
  status?: number
}

/** 创建用户输入 */
export interface CreateUserInput {
  username: string
  password: string
  realName: string
  roleId: number
  classId?: number
  phone?: string
  email?: string
  status?: number
}

/** 用户删除影响清单 */
export interface V1UserDeletionImpact {
  resourceType: string
  resourceId: string
  referenceCount: number
  references: string[]
  deletionBlocked: boolean
}

/**
 * 将 v1 分页响应转换为旧版分页格式 { records, total }
 * 使 useApiList 的默认解析逻辑兼容 v1 响应
 */
export function toLegacyPage<T>(v1Page: V1Page<T> | undefined): { records: T[]; total: number } {
  if (!v1Page) return { records: [], total: 0 }
  return {
    records: v1Page.items ?? [],
    total: v1Page.pageInfo?.total ?? 0,
  }
}

// ===== 用户管理 =====

export function getUsers(params: { page?: number; size?: number; keyword?: string; roleId?: number; status?: number }) {
  return request.get('/api/v1/users', { params })
}

export function createUser(data: CreateUserInput) {
  return request.post('/api/v1/users', {
    username: data.username,
    password: data.password,
    realName: data.realName,
    roleId: data.roleId,
    classId: data.classId,
    phone: data.phone,
    email: data.email,
  })
}

export function getUserById(id: string | number) {
  return request.get(`/api/v1/users/${id}`)
}

export function updateUser(
  id: string | number,
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
  return request.put(`/api/v1/users/${id}`, {
    username: data.username,
    realName: data.realName,
    roleId: data.roleId,
    classId: data.classId,
    phone: data.phone,
    email: data.email,
    password: data.password,
  })
}

/** 更新用户状态 — v1 使用 JSON body 而非 query params */
export function updateStatus(id: string | number, status: number) {
  return request.put(`/api/v1/users/${id}/status`, { status })
}

/** 获取用户删除影响 — 使用 Orval 已生成的函数 */
export { deletionImpact as getUserDeletionImpact } from '@/shared/api/generated/v-1-user-approval-controller/v-1-user-approval-controller'

/** 删除用户 — 使用 Orval 已生成的函数 */
export { delete1 as deleteUser } from '@/shared/api/generated/v-1-user-approval-controller/v-1-user-approval-controller'

export function batchImportUsers(users: CreateUserInput[]) {
  return request.post('/api/v1/users/batch', users, { timeout: 120000 })
}

export function parseAiImportUsers(messages: AiImportMessage[]) {
  return request.post('/api/v1/users/batch/ai-parse', { messages })
}

// ===== 班级/字典/参考数据 =====

/** 获取班级列表 — v1 端点 */
export function getClasses() {
  return request.get('/api/v1/classes')
}

// ===== 角色管理 =====

export function getRoles(params: { page?: number; size?: number; excludeSystem?: boolean }) {
  return request.get('/api/v1/roles', { params })
}

export function getRoleDetail(id: string | number) {
  return request.get(`/api/v1/roles/${id}`)
}

export function createRole(data: { roleName: string; roleCode: string; description?: string }) {
  return request.post('/api/v1/roles', data)
}

export function updateRole(id: string | number, data: { roleName?: string; roleCode?: string; description?: string }) {
  return request.put(`/api/v1/roles/${id}`, data)
}

export function deleteRole(id: string | number) {
  return request.delete(`/api/v1/roles/${id}`)
}

export function getRoleMenus(id: string | number) {
  return request.get(`/api/v1/roles/${id}/menus`)
}

export function updateRoleMenus(id: string | number, menuIds: number[]) {
  return request.put(`/api/v1/roles/${id}/menus`, menuIds)
}

// ===== 菜单管理 =====

export function getMenuTree() {
  return request.get('/api/v1/menus/tree')
}

export function getMenuById(id: string | number) {
  return request.get(`/api/v1/menus/${id}`)
}

export function createMenu(data: { menuName: string; parentId?: string; path?: string; permission?: string; type?: string; icon?: string; sort?: number }) {
  return request.post('/api/v1/menus', data)
}

export function updateMenu(id: string | number, data: { menuName?: string; parentId?: string; path?: string; permission?: string; type?: string; icon?: string; sort?: number }) {
  return request.put(`/api/v1/menus/${id}`, data)
}
