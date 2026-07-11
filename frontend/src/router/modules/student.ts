import type { RouteRecordRaw } from 'vue-router'

const studentRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/student/Login.vue'),
    meta: { title: '登录', noAuth: true },
  },
  {
    path: '/student',
    component: () => import('@/layout/StudentLayout.vue'),
    meta: { title: '学生端', roles: ['student'] },
    redirect: '/student/home',
    children: [
      {
        path: 'home',
        name: 'StudentHome',
        component: () => import('@/views/student/Home.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'works',
        name: 'MyWorks',
        component: () => import('@/views/student/MyWorks.vue'),
        meta: { title: '我的作品' },
      },
      {
        path: 'works/create',
        name: 'WorkCreate',
        component: () => import('@/views/student/WorkSubmit.vue'),
        meta: { title: '提交作品' },
      },
      {
        path: 'todos',
        name: 'TodoList',
        component: () => import('@/views/student/TodoList.vue'),
        meta: { title: '我的待办' },
      },
      {
        path: 'works/edit/:id',
        name: 'WorkEdit',
        component: () => import('@/views/student/WorkSubmit.vue'),
        meta: { title: '编辑作品' },
      },
      {
        path: 'works/view/:id',
        name: 'WorkView',
        component: () => import('@/views/student/WorkSubmit.vue'),
        meta: { title: '作品详情' },
      },
      {
        path: 'ranking',
        name: 'MyRanking',
        component: () => import('@/views/student/MyRanking.vue'),
        meta: { title: '我的评分' },
      },
      {
        path: 'notify',
        name: 'StudentNotify',
        component: () => import('@/views/student/Notify.vue'),
        meta: { title: '消息通知' },
      },
    ],
  },
]

export default studentRoutes
