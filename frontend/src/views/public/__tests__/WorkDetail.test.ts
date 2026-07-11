import { beforeEach, describe, expect, it, vi } from 'vitest'
import WorkDetail from '../WorkDetail.vue'
import { mountView, CommentThreadStub } from '../../__tests__/test-utils'

const {
  routeMock,
  pushMock,
  infoMock,
  successMock,
  errorMock,
  confirmMock,
  getPublicWorkDetailMock,
  toggleLikeMock,
  getCommentListMock,
  addCommentMock,
  deleteCommentMock,
} = vi.hoisted(() => ({
  routeMock: { params: { id: '9' } },
  pushMock: vi.fn(),
  infoMock: vi.fn(),
  successMock: vi.fn(),
  errorMock: vi.fn(),
  confirmMock: vi.fn(),
  getPublicWorkDetailMock: vi.fn(),
  toggleLikeMock: vi.fn(),
  getCommentListMock: vi.fn(),
  addCommentMock: vi.fn(),
  deleteCommentMock: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => routeMock,
  useRouter: () => ({ push: pushMock }),
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: {
      info: infoMock,
      success: successMock,
      error: errorMock,
    },
    ElMessageBox: {
      confirm: confirmMock,
    },
  }
})

vi.mock('@/api/public/work', () => ({
  getPublicWorkDetail: getPublicWorkDetailMock,
  toggleLike: toggleLikeMock,
}))

vi.mock('@/api/public/comment', () => ({
  getCommentList: getCommentListMock,
  addComment: addCommentMock,
  deleteComment: deleteCommentMock,
}))

describe('Public WorkDetail view', () => {
  beforeEach(() => {
    sessionStorage.clear()
    localStorage.clear()
    routeMock.params = { id: '9' }
    pushMock.mockReset()
    infoMock.mockReset()
    successMock.mockReset()
    errorMock.mockReset()
    confirmMock.mockReset()
    getPublicWorkDetailMock.mockReset()
    toggleLikeMock.mockReset()
    getCommentListMock.mockReset()
    addCommentMock.mockReset()
    deleteCommentMock.mockReset()

    getPublicWorkDetailMock.mockResolvedValue({
      data: {
        id: 9,
        title: '作品详情',
        submitterName: '张三',
        advisor: '李老师',
        submitTime: '2026-05-25',
        summary: '介绍',
        techStack: 'Vue3,Spring Boot',
        liked: false,
        likeCount: 3,
        viewCount: 20,
        members: [{ studentId: 1, studentName: '张三', studentNo: '2022001', className: '软工1班', isLeader: true }],
        attachments: [{ id: 1, fileName: 'demo.zip', fileUrl: 'https://cdn/demo.zip' }],
        tags: ['Vue3'],
      },
    })
    getCommentListMock.mockResolvedValue({
      data: {
        records: [{ id: 1, content: '不错', userId: 1, replies: [] }],
        total: 1,
      },
    })
  })

  it('loads detail and comments on mount', async () => {
    const wrapper = await mountView(WorkDetail, {
      global: {
        stubs: {
          CommentThread: CommentThreadStub,
        },
      },
    })

    expect(getPublicWorkDetailMock).toHaveBeenCalledWith('9')
    expect(getCommentListMock).toHaveBeenCalledWith('9', 1, 10)
    expect(wrapper.text()).toContain('作品详情')
    expect(wrapper.text()).toContain('不错')
  })

  it('redirects anonymous users to login when liking', async () => {
    const wrapper = await mountView(WorkDetail, {
      global: {
        stubs: {
          CommentThread: CommentThreadStub,
        },
      },
    })

    await (wrapper.vm as any).handleLike()

    expect(infoMock).toHaveBeenCalledWith('登录后可点赞')
    expect(pushMock).toHaveBeenCalledWith('/login')
  })

  it('toggles like for logged-in users', async () => {
    sessionStorage.setItem('token', 'token')
    localStorage.setItem('userInfo', JSON.stringify({ id: 1, roleCode: 'ROLE_STUDENT' }))
    toggleLikeMock.mockResolvedValue({ data: { liked: true, likeCount: 4 } })
    const wrapper = await mountView(WorkDetail, {
      global: {
        stubs: {
          CommentThread: CommentThreadStub,
        },
      },
    })

    await (wrapper.vm as any).handleLike()

    expect(toggleLikeMock).toHaveBeenCalledWith('9')
    expect((wrapper.vm as any).liked).toBe(true)
    expect((wrapper.vm as any).likeCount).toBe(4)
  })

  it('submits a new comment and refreshes comment list', async () => {
    sessionStorage.setItem('token', 'token')
    localStorage.setItem('userInfo', JSON.stringify({ id: 1, roleCode: 'ROLE_STUDENT' }))
    addCommentMock.mockResolvedValue({})
    const wrapper = await mountView(WorkDetail, {
      global: {
        stubs: {
          CommentThread: CommentThreadStub,
        },
      },
    })

    ;(wrapper.vm as any).commentText = '新的评论'
    await (wrapper.vm as any).submitComment()

    expect(addCommentMock).toHaveBeenCalledWith('9', '新的评论')
    expect(successMock).toHaveBeenCalledWith('评论成功')
    expect(getCommentListMock).toHaveBeenCalledTimes(2)
  })
})
