import { beforeEach, describe, expect, it, vi } from 'vitest'
import WorkSubmit from '../WorkSubmit.vue'
import { mountView } from '../../__tests__/test-utils'

const {
  routeMock,
  pushMock,
  backMock,
  successMock,
  warningMock,
  errorMock,
  createWorkMock,
  updateWorkMock,
  submitWorkMock,
  getWorkDetailMock,
  uploadFileMock,
} = vi.hoisted(() => ({
  routeMock: { name: 'WorkCreate', params: {} as Record<string, any> },
  pushMock: vi.fn(),
  backMock: vi.fn(),
  successMock: vi.fn(),
  warningMock: vi.fn(),
  errorMock: vi.fn(),
  createWorkMock: vi.fn(),
  updateWorkMock: vi.fn(),
  submitWorkMock: vi.fn(),
  getWorkDetailMock: vi.fn(),
  uploadFileMock: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => routeMock,
  useRouter: () => ({
    push: pushMock,
    back: backMock,
  }),
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: successMock,
      warning: warningMock,
      error: errorMock,
    },
  }
})

vi.mock('@/api/student/work', () => ({
  createWork: createWorkMock,
  updateWork: updateWorkMock,
  submitWork: submitWorkMock,
  getWorkDetail: getWorkDetailMock,
  uploadFile: uploadFileMock,
}))

describe('WorkSubmit view', () => {
  beforeEach(() => {
    routeMock.name = 'WorkCreate'
    routeMock.params = {}
    pushMock.mockReset()
    backMock.mockReset()
    successMock.mockReset()
    warningMock.mockReset()
    errorMock.mockReset()
    createWorkMock.mockReset()
    updateWorkMock.mockReset()
    submitWorkMock.mockReset()
    getWorkDetailMock.mockReset()
    uploadFileMock.mockReset()
  })

  it('submits a new work after validation passes', async () => {
    createWorkMock.mockResolvedValue({ data: 88 })
    submitWorkMock.mockResolvedValue({})
    const wrapper = await mountView(WorkSubmit)

    ;(wrapper.vm as any).formRef = { validate: vi.fn().mockResolvedValue(true) }
    ;(wrapper.vm as any).form.title = '智能导览'
    ;(wrapper.vm as any).form.summary = '简介'
    ;(wrapper.vm as any).form.techStack = 'Vue3'
    ;(wrapper.vm as any).form.members[0].studentName = '张三'
    ;(wrapper.vm as any).form.members[0].studentNo = '2022001'
    ;(wrapper.vm as any).form.attachments = [
      { id: 1, fileName: 'demo.mp4', fileType: 'mp4', fileUrl: 'https://cdn/demo.mp4' },
    ]

    await (wrapper.vm as any).handleSaveAndSubmit()

    expect(createWorkMock).toHaveBeenCalled()
    expect(submitWorkMock).toHaveBeenCalledWith(88)
    expect(successMock).toHaveBeenCalledWith('提交成功，请等待管理员审核')
    expect(pushMock).toHaveBeenCalledWith('/student/works')
  })

  it('uploads attachments and syncs file list into the form', async () => {
    uploadFileMock.mockResolvedValue({ data: { id: 5, url: 'https://cdn/demo.zip' } })
    const wrapper = await mountView(WorkSubmit)
    const file = new File(['demo'], 'demo.zip', { type: 'application/zip' })

    const result = await (wrapper.vm as any).beforeFileUpload(file)

    expect(result).toBe(false)
    expect(uploadFileMock).toHaveBeenCalled()
    expect((wrapper.vm as any).form.attachments).toHaveLength(1)
    expect((wrapper.vm as any).fileList).toHaveLength(1)
    expect(successMock).toHaveBeenCalled()
  })

  it('adds members and keeps only one leader', async () => {
    const wrapper = await mountView(WorkSubmit)

    ;(wrapper.vm as any).addMember()
    expect((wrapper.vm as any).form.members).toHaveLength(2)
    ;(wrapper.vm as any).form.members[1].isLeader = true
    ;(wrapper.vm as any).handleLeaderChange(1)

    expect((wrapper.vm as any).form.members[0].isLeader).toBe(false)
    expect((wrapper.vm as any).form.members[1].isLeader).toBe(true)
  })

  it('loads existing work detail in edit mode', async () => {
    routeMock.name = 'WorkEdit'
    routeMock.params = { id: '12' }
    getWorkDetailMock.mockResolvedValue({
      data: {
        title: '旧作品',
        summary: '旧简介',
        techStack: 'Vue3,Spring Boot',
        advisor: '李老师',
        coverUrl: 'cover.png',
        videoUrl: '',
        runDescription: 'run',
        members: [{ studentName: '张三', studentNo: '2022001', className: '软工1班', isLeader: true }],
        attachments: [{ id: 1, fileName: 'demo.zip', fileUrl: 'https://cdn/demo.zip' }],
      },
    })

    const wrapper = await mountView(WorkSubmit)

    expect(getWorkDetailMock).toHaveBeenCalledWith('12')
    expect((wrapper.vm as any).form.title).toBe('旧作品')
    expect((wrapper.vm as any).fileList).toHaveLength(1)
  })
})
