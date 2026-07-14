import { beforeEach, describe, expect, it, vi } from 'vitest'

const { requestGet, requestPost, requestPut, requestDelete } = vi.hoisted(() => ({
  requestGet: vi.fn(),
  requestPost: vi.fn(),
  requestPut: vi.fn(),
  requestDelete: vi.fn(),
}))

vi.mock('../../request', () => ({
  default: {
    get: requestGet,
    post: requestPost,
    put: requestPut,
    delete: requestDelete,
  },
}))

import { getMenuTree } from '../menu'
import { getRoles } from '../role'
import { getClasses, getUsers } from '../user'

describe('管理端身份与参考数据接口', () => {
  beforeEach(() => {
    requestGet.mockReset()
  })

  it('使用 v1 菜单接口', async () => {
    requestGet.mockResolvedValue({ data: [] })

    await getMenuTree()

    expect(requestGet).toHaveBeenCalledWith('/api/v1/menus/tree')
  })

  it('将 v1 用户分页响应转换为页面所需格式', async () => {
    requestGet.mockResolvedValue({
      data: { items: [{ id: '9007199254740993', username: 'student' }], pageInfo: { total: 1 } },
    })

    const response = await getUsers({ page: 1, size: 20 })

    expect(requestGet).toHaveBeenCalledWith('/api/v1/users', { params: { page: 1, size: 20 } })
    expect(response.data).toEqual({ records: [{ id: '9007199254740993', username: 'student' }], total: 1 })
  })

  it('将 v1 班级参考数据转换为下拉框选项', async () => {
    requestGet.mockResolvedValue({ data: [{ id: '3', label: '软件工程 1 班' }] })

    const response = await getClasses()

    expect(requestGet).toHaveBeenCalledWith('/api/v1/classes')
    expect(response.data).toEqual([{ id: 3, className: '软件工程 1 班' }])
  })

  it('将 v1 角色分页响应转换为角色列表', async () => {
    requestGet.mockResolvedValue({ data: { items: [{ id: '2', roleName: '教师' }], pageInfo: { total: 1 } } })

    const response = await getRoles({ page: 1, size: 10, excludeSystem: true })

    expect(requestGet).toHaveBeenCalledWith('/api/v1/roles', {
      params: { page: 1, size: 10, excludeSystem: true },
    })
    expect(response.data).toEqual({ records: [{ id: '2', roleName: '教师' }], total: 1 })
  })
})
