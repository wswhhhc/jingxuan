import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuditPage from '../index.vue'
import { mountView } from '../../../__tests__/test-utils'

const {
  successMock,
  confirmMock,
  requestGetMock,
  getAuditListMock,
  doAuditMock,
  getAuditDetailMock,
  getAuditHistoryMock,
  publishWorkMock,
  offlineWorkMock,
  setFeaturedMock,
} = vi.hoisted(() => ({
  successMock: vi.fn(),
  confirmMock: vi.fn(),
  requestGetMock: vi.fn(),
  getAuditListMock: vi.fn(),
  doAuditMock: vi.fn(),
  getAuditDetailMock: vi.fn(),
  getAuditHistoryMock: vi.fn(),
  publishWorkMock: vi.fn(),
  offlineWorkMock: vi.fn(),
  setFeaturedMock: vi.fn(),
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: successMock,
    },
    ElMessageBox: {
      confirm: confirmMock,
    },
  }
})

vi.mock('@/api/request', () => ({
  default: {
    get: requestGetMock,
  },
}))

vi.mock('@/api/admin/audit', () => ({
  getAuditList: getAuditListMock,
  doAudit: doAuditMock,
  getAuditDetail: getAuditDetailMock,
  getAuditHistory: getAuditHistoryMock,
  publishWork: publishWorkMock,
  offlineWork: offlineWorkMock,
  setFeatured: setFeaturedMock,
}))

describe('Admin audit view', () => {
  beforeEach(() => {
    successMock.mockReset()
    confirmMock.mockReset()
    requestGetMock.mockReset()
    getAuditListMock.mockReset()
    doAuditMock.mockReset()
    getAuditDetailMock.mockReset()
    getAuditHistoryMock.mockReset()
    publishWorkMock.mockReset()
    offlineWorkMock.mockReset()
    setFeaturedMock.mockReset()

    requestGetMock.mockResolvedValue({ data: [{ id: 1, dictLabel: '软工1班' }] })
    getAuditListMock.mockResolvedValue({
      data: {
        records: [
          {
            id: 5,
            title: '待审作品',
            submitterName: '张三',
            techStack: 'Vue',
            submitTime: '2026-05-25T10:00:00',
            status: 1,
            publishStatus: 0,
          },
        ],
        total: 1,
      },
    })
    getAuditDetailMock.mockResolvedValue({
      data: {
        id: 5,
        title: '待审作品',
        submitterName: '张三',
        techStack: 'Vue',
        advisor: '李老师',
        submitTime: '2026-05-25T10:00:00',
        publishStatus: 0,
        publishStatusLabel: '未发布',
        summary: '摘要',
        status: 1,
        featured: 0,
        previewUrl: '',
      },
    })
    getAuditHistoryMock.mockResolvedValue({
      data: { records: [{ id: 1, result: 1, reason: '', auditorName: '管理员', auditTime: '2026-05-25' }] },
    })
  })

  it('loads classes and audit list on mount', async () => {
    const wrapper = await mountView(AuditPage)

    expect(requestGetMock).toHaveBeenCalledWith('/api/admin/dict/classes')
    expect(getAuditListMock).toHaveBeenCalled()
    expect(wrapper.text()).toContain('审核列表')
  })

  it('opens detail dialog and loads history', async () => {
    const wrapper = await mountView(AuditPage)

    await (wrapper.vm as any).showDetail({ id: 5 })

    expect(getAuditDetailMock).toHaveBeenCalledWith(5)
    expect(getAuditHistoryMock).toHaveBeenCalledWith(5)
    expect((wrapper.vm as any).detailVisible).toBe(true)
  })

  it('submits audit result and refreshes list', async () => {
    doAuditMock.mockResolvedValue({})
    const wrapper = await mountView(AuditPage)

    await (wrapper.vm as any).submitAudit(5, 'approved')

    expect(doAuditMock).toHaveBeenCalledWith({ workId: 5, result: 'approved', reason: undefined })
    expect(successMock).toHaveBeenCalledWith('审核通过')
    expect(getAuditListMock).toHaveBeenCalledTimes(2)
  })

  it('publishes and updates featured state from detail dialog', async () => {
    publishWorkMock.mockResolvedValue({})
    setFeaturedMock.mockResolvedValue({})
    const wrapper = await mountView(AuditPage)
    ;(wrapper.vm as any).detail = { id: 5, title: '待审作品', featured: 0, previewUrl: '', publishStatus: 1 }

    await (wrapper.vm as any).handlePublish(5)
    ;(wrapper.vm as any).handleFeatured({ title: '待审作品', featured: 0, previewUrl: 'https://demo' })
    await (wrapper.vm as any).handleFeaturedSubmit()

    expect(publishWorkMock).toHaveBeenCalledWith(5)
    expect(setFeaturedMock).toHaveBeenCalledWith(5, 0, 'https://demo')
  })
})
