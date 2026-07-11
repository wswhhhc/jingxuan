import type { RouteRecordRaw } from 'vue-router'

const publicRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layout/PublicLayout.vue'),
    meta: { noAuth: true },
    redirect: '/works',
    children: [
      {
        path: 'works',
        name: 'PublicWorkList',
        component: () => import('@/views/public/WorkList.vue'),
        meta: { title: 'Works' },
      },
      {
        path: 'works/:id',
        name: 'PublicWorkDetail',
        component: () => import('@/views/public/WorkDetail.vue'),
        meta: { title: 'Work Detail' },
      },
      {
        path: 'ranking',
        name: 'PublicRanking',
        component: () => import('@/views/public/Ranking.vue'),
        meta: { title: 'Ranking' },
      },
    ],
  },
]

export default publicRoutes
