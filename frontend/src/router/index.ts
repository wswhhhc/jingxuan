import { createRouter, createWebHistory, type RouteMeta } from 'vue-router'
import studentRoutes from './modules/student'
import publicRoutes from './modules/public'
import adminRoutes from './modules/admin'
import teacherRoutes from './modules/teacher'
import { getAuthToken, getCachedUserInfo } from '@/utils/auth'

const routes = [
  ...publicRoutes,
  ...studentRoutes,
  ...adminRoutes,
  ...teacherRoutes,
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { title: '注册', noAuth: true },
  },
  {
    path: '/change-password',
    name: 'ChangePassword',
    component: () => import('../views/ChangePassword.vue'),
    meta: { title: '修改密码' },
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { title: '个人信息' },
  },
]

const router = createRouter({
  history: createWebHistory('/jingxuan/'),
  routes,
})

export function resolveAuthRedirect(to: { meta: RouteMeta; matched: Array<{ meta?: RouteMeta }> }): string | undefined {
  const token = getAuthToken()

  if (to.meta.noAuth) {
    return undefined
  }

  if (!token) {
    return '/login'
  }

  const matchedRoles = to.matched.flatMap((r) => (r.meta?.roles as string[]) || [])
  if (matchedRoles.length === 0) {
    return undefined
  }

  const userInfo = getCachedUserInfo()

  if (!userInfo) {
    return '/login'
  }

  const roleCode = (userInfo.roleCode || '').replace('ROLE_', '').toLowerCase()
  if (!matchedRoles.includes(roleCode)) {
    return '/login'
  }

  return undefined
}

router.beforeEach((to, _from, next) => {
  const redirect = resolveAuthRedirect(to)
  if (redirect) {
    next(redirect)
    return
  }
  next()
})

export default router
