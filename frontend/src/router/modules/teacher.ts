import type { RouteRecordRaw } from 'vue-router'

const teacherRoutes: RouteRecordRaw[] = [
  {
    path: '/teacher',
    component: () => import('@/layout/TeacherLayout.vue'),
    redirect: '/teacher/dashboard',
    meta: { roles: ['teacher'] },
    children: [
      {
        path: 'dashboard',
        name: 'TeacherDashboard',
        component: () => import('@/views/teacher/dashboard/index.vue'),
        meta: { title: '教师工作台' },
      },
      {
        path: 'score',
        name: 'TeacherScore',
        component: () => import('@/views/teacher/score/index.vue'),
        meta: { title: '作品评分' },
      },
      {
        path: 'history',
        name: 'TeacherHistory',
        component: () => import('@/views/teacher/history/index.vue'),
        meta: { title: '我的评分记录' },
      },
      {
        path: 'ranking',
        name: 'TeacherRanking',
        component: () => import('@/views/teacher/ranking/index.vue'),
        meta: { title: '排行榜' },
      },
      {
        path: 'notify',
        name: 'TeacherNotify',
        component: () => import('@/views/teacher/notify/index.vue'),
        meta: { title: '消息通知' },
      },
    ],
  },
]

export default teacherRoutes
