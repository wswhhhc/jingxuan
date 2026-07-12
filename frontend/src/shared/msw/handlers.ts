import { http, HttpResponse } from 'msw'

/** v1 API 测试用 handler 集合 */
export const handlers = [
  // 认证
  http.post('/api/v1/auth/login', () =>
    HttpResponse.json({
      accessToken: 'test-token',
      tokenType: 'Bearer',
      expiresIn: 900,
      user: {
        id: '1',
        username: 'admin',
        realName: '管理员',
        roleId: 3,
        roleCode: 'ADMIN',
      },
    }),
  ),

  // 作品列表
  http.get('/api/v1/showcase/works', () =>
    HttpResponse.json({
      items: [
        {
          id: '1',
          title: '智慧校园',
          summary: '简介',
          techStack: 'Vue',
          submitterName: '张三',
          status: 3,
          submitTime: '2026-06-01T12:00:00+08:00',
          featured: 1,
          likeCount: 6,
          viewCount: 12,
        },
      ],
      pageInfo: { page: 1, pageSize: 12, total: 1, totalPages: 1 },
    }),
  ),

  // 作品详情
  http.get('/api/v1/showcase/works/:id', () =>
    HttpResponse.json({
      id: '1',
      title: '智慧校园',
      summary: '简介',
      techStack: 'Vue,Spring Boot',
      submitterName: '张三',
      status: 'APPROVED',
      attachments: [],
      members: [],
      likeCount: 6,
      viewCount: 12,
    }),
  ),

  // 公告列表
  http.get('/api/v1/notices', () =>
    HttpResponse.json({
      items: [{ id: '1', title: '公告1', status: 'PUBLISHED', targetScope: 'all' }],
      pageInfo: { page: 1, pageSize: 20, total: 1, totalPages: 1 },
    }),
  ),

  // 仪表盘
  http.get('/api/v1/dashboard/stats', () =>
    HttpResponse.json({
      totalWorks: 100,
      pendingAudit: 10,
      publishedWorks: 80,
      totalTeachers: 20,
      totalStudents: 500,
      activeBatches: 2,
      recentWorks: [],
      scoreDistribution: {},
    }),
  ),

  http.get('/api/v1/dashboard/charts', () =>
    HttpResponse.json({
      techStackDistribution: [],
      statusDistribution: {},
      scoreDistribution: [],
    }),
  ),

  // 通知未读数
  http.get('/api/v1/me/notifications/unread-count', () =>
    HttpResponse.json({ count: 3 }),
  ),

  // 班级
  http.get('/api/v1/classes', () =>
    HttpResponse.json([{ id: 1, label: '软工1班' }]),
  ),

  // 标签
  http.get('/api/v1/tags', () =>
    HttpResponse.json([{ id: 1, name: 'Vue', type: 'tech' }]),
  ),

  // 排行榜
  http.get('/api/v1/leaderboards', () =>
    HttpResponse.json([
      { rankNo: 1, workId: 1, workTitle: '作品A', avgScore: 95, avgInnovation: 24, avgDifficulty: 24, avgCompletion: 28, avgPracticality: 19, teacherCount: 3, rewardLevel: '一等奖' },
    ]),
  ),

  // 日志
  http.get('/api/v1/audit-logs', () =>
    HttpResponse.json({
      items: [{ id: '1', userId: '1', username: 'admin', action: '登录', success: true, createdAt: '2026-06-01T00:00:00+08:00' }],
      pageInfo: { page: 1, pageSize: 20, total: 1, totalPages: 1 },
    }),
  ),

  // 我的待办
  http.get('/api/v1/me/tasks', () =>
    HttpResponse.json([
      { id: 1, title: '提交作品', status: 0, batchName: '2026春' },
    ]),
  ),
]
