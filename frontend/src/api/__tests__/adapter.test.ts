import { describe, it, expect } from 'vitest'
import {
  STATUS_MAP,
  PUBLISH_STATUS_MAP,
  adaptWorkVO,
  adaptPageResult,
  adaptAttachment,
  adaptMember,
} from '../workAdapter'

const mockWorkItem = {
  id: '1912345678901234567',
  title: '测试作品',
  summary: '测试简介',
  techStack: 'Java/Spring Boot',
  advisor: '张教授',
  coverUrl: '/uploads/cover.jpg',
  videoUrl: '/uploads/demo.mp4',
  runDesc: '运行说明',
  status: 3,
  statusLabel: '已通过',
  submitterId: 100,
  submitterName: '张三',
  submitTime: '2025-03-15 10:00:00',
  publishStatus: 1,
  featured: 1,
  likeCount: 10,
  viewCount: 100,
  liked: true,
  tags: ['Java', 'Vue'],
  memberCount: 2,
  avgScore: '85.50',
  rank: 2,
  previewUrl: 'http://preview.example.com',
  teacherCount: 3,
  avgInnovation: '20.5',
  avgDifficulty: '18.0',
  avgCompletion: '25.0',
  avgPracticality: '15.0',
  members: [
    { id: 1, studentId: 100, studentName: '张三', studentNo: '2022001', isLeader: 1 },
    { id: 2, studentId: 101, studentName: '李四', studentNo: '2022002', isLeader: 0 },
  ],
  attachments: [{ id: 1, fileName: 'report.pdf', fileType: 'pdf', fileUrl: '/uploads/report.pdf' }],
}

describe('API 适配层 — workAdapter.ts', () => {
  describe('枚举映射', () => {
    it('后端 status 0/1/2/3 映射到前端字符串', () => {
      expect(STATUS_MAP[0]).toBe('draft')
      expect(STATUS_MAP[1]).toBe('submitted')
      expect(STATUS_MAP[2]).toBe('rejected')
      expect(STATUS_MAP[3]).toBe('approved')
    })

    it('publishStatus 0/1/2 映射正确', () => {
      expect(PUBLISH_STATUS_MAP[0]).toBe('unpublished')
      expect(PUBLISH_STATUS_MAP[1]).toBe('published')
      expect(PUBLISH_STATUS_MAP[2]).toBe('offline')
    })

    it('未知 publishStatus 映射为 undefined', () => {
      expect(PUBLISH_STATUS_MAP[99]).toBeUndefined()
    })
  })

  describe('adaptAttachment', () => {
    it('基础字段映射', () => {
      const result = adaptAttachment({ id: 1, fileName: 'test.pdf', fileType: 'pdf', fileUrl: '/url' })
      expect(result.fileName).toBe('test.pdf')
      expect(result.fileType).toBe('pdf')
      expect(result.fileUrl).toBe('/url')
    })

    it('空值保护', () => {
      const result = adaptAttachment({})
      expect(result.fileName).toBe('')
      expect(result.fileType).toBe('')
      expect(result.fileUrl).toBe('')
    })
  })

  describe('adaptMember', () => {
    it('isLeader 数字 1 转布尔 true', () => {
      const result = adaptMember({ studentName: 'A', isLeader: 1 })
      expect(result.isLeader).toBe(true)
    })

    it('isLeader 数字 0 转布尔 false', () => {
      const result = adaptMember({ studentName: 'B', isLeader: 0 })
      expect(result.isLeader).toBe(false)
    })

    it('isLeader 布尔 true 保持不变', () => {
      const result = adaptMember({ studentName: 'C', isLeader: true })
      expect(result.isLeader).toBe(true)
    })

    it('空值保护', () => {
      const result = adaptMember({})
      expect(result.studentName).toBe('')
      expect(result.studentNo).toBe('')
      expect(result.className).toBe('')
      expect(result.isLeader).toBe(false)
    })
  })

  describe('adaptWorkVO', () => {
    it('runDesc 转为 runDescription', () => {
      const adapted = adaptWorkVO(mockWorkItem)
      expect(adapted.runDescription).toBe('运行说明')
      expect((adapted as any).runDesc).toBeUndefined()
    })

    it('status 转为前端字符串', () => {
      const adapted = adaptWorkVO(mockWorkItem)
      expect(adapted.status).toBe('approved')
    })

    it('score/rank/previewUrl 正常映射', () => {
      const adapted = adaptWorkVO(mockWorkItem)
      expect(adapted.score).toBe(85.5)
      expect(adapted.rank).toBe(2)
      expect(adapted.previewUrl).toBe('http://preview.example.com')
    })

    it('avgScore 为 null 时 score 为 undefined', () => {
      const adapted = adaptWorkVO({ ...mockWorkItem, avgScore: null })
      expect(adapted.score).toBeUndefined()
    })

    it('isLeader 正确转换', () => {
      const adapted = adaptWorkVO(mockWorkItem)
      const leader = adapted.members.find((member) => member.studentId === 100)!
      const member = adapted.members.find((item) => item.studentId === 101)!
      expect(leader.isLeader).toBe(true)
      expect(member.isLeader).toBe(false)
    })

    it('attachments 映射', () => {
      const adapted = adaptWorkVO(mockWorkItem)
      expect(adapted.attachments).toHaveLength(1)
      expect(adapted.attachments[0].fileName).toBe('report.pdf')
    })

    it('likeCount/viewCount 空保护', () => {
      const adapted = adaptWorkVO({ ...mockWorkItem, likeCount: null, viewCount: undefined })
      expect(adapted.likeCount).toBe(0)
      expect(adapted.viewCount).toBe(0)
    })

    it('liked 空保护', () => {
      const adapted = adaptWorkVO({ ...mockWorkItem, liked: undefined })
      expect(adapted.liked).toBe(false)
    })
  })

  describe('adaptPageResult', () => {
    const adapter = (item: any) => ({ title: item.title })

    it('records 空保护', () => {
      const result = adaptPageResult({ records: null, total: 0, pageNum: 1, pageSize: 12 }, adapter)
      expect(result.records).toEqual([])
      expect(result.total).toBe(0)
    })

    it('pageNum 映射为 page', () => {
      const result = adaptPageResult({ records: [], total: 1, pageNum: 2, pageSize: 10 }, adapter)
      expect(result.page).toBe(2)
      expect(result.pageSize).toBe(10)
    })

    it('兼容 v1 的 items 和 pageInfo 分页结构', () => {
      const result = adaptPageResult(
        {
          items: [mockWorkItem],
          pageInfo: { page: 2, pageSize: 12, total: 25 },
        },
        adaptWorkVO,
      )

      expect(result.records).toHaveLength(1)
      expect(result.total).toBe(25)
      expect(result.page).toBe(2)
      expect(result.pageSize).toBe(12)
    })

    it('缺少 pageNum 时回退到 page', () => {
      const result = adaptPageResult({ records: [], total: 0, page: 3, pageSize: 20 }, adapter)
      expect(result.page).toBe(3)
    })

    it('使用自定义 adapter 转换 records', () => {
      const result = adaptPageResult({ records: [mockWorkItem], total: 1, pageNum: 1, pageSize: 10 }, adaptWorkVO)
      expect(result.records[0].title).toBe('测试作品')
      expect(result.records[0].status).toBe('approved')
    })
  })
})
