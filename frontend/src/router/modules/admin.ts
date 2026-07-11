import type { RouteRecordRaw } from 'vue-router'

const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { roles: ['admin'] },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/dashboard/index.vue'),
        meta: { title: '控制台' },
      },
      {
        path: 'audit',
        name: 'AdminAudit',
        component: () => import('@/views/admin/audit/index.vue'),
        meta: { title: '审核管理' },
      },
      {
        path: 'notice',
        name: 'AdminNotice',
        component: () => import('@/views/admin/notice/index.vue'),
        meta: { title: '公告管理' },
      },
      {
        path: 'comment',
        name: 'AdminComment',
        component: () => import('@/views/admin/comment/index.vue'),
        meta: { title: '评论管理' },
      },
      {
        path: 'rules',
        name: 'AdminRules',
        component: () => import('@/views/admin/rule/index.vue'),
        meta: { title: '审核规则' },
      },
      {
        path: 'prize',
        name: 'AdminPrize',
        component: () => import('@/views/admin/prize/index.vue'),
        meta: { title: '奖品配置' },
      },
      {
        path: 'score-batch',
        name: 'AdminScoreBatch',
        component: () => import('@/views/admin/scoreBatch/index.vue'),
        meta: { title: '评分批次' },
      },
      {
        path: 'roles',
        name: 'AdminRoles',
        component: () => import('@/views/admin/role/index.vue'),
        meta: { title: '角色权限' },
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/user/index.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'notify',
        name: 'AdminNotify',
        component: () => import('@/views/admin/notify/index.vue'),
        meta: { title: '消息通知' },
      },
      {
        path: 'log',
        name: 'AdminLog',
        component: () => import('@/views/admin/log/index.vue'),
        meta: { title: '操作日志' },
      },
      {
        path: 'dict',
        name: 'AdminDict',
        component: () => import('@/views/admin/dict/index.vue'),
        meta: { title: '数据字典' },
      },
    ],
  },
]

export default adminRoutes
