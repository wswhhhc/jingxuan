<template>
  <div class="workspace-layout teacher-layout">
    <div class="workspace-layout__frame">
      <aside class="workspace-layout__sidebar">
        <div class="workspace-layout__brand">
          <span class="workspace-layout__brand-note">Review Chamber</span>
          <span class="workspace-layout__brand-title">教师评审席</span>
          <span class="workspace-layout__brand-desc"
            >评审视角保持克制与清晰，让评分、记录与通知都围绕作品材料展开。</span
          >
        </div>

        <el-menu :default-active="route.path" router class="workspace-layout__menu">
          <el-menu-item index="/teacher/dashboard">
            <el-icon><HomeFilled /></el-icon>
            <span>工作台</span>
          </el-menu-item>
          <el-menu-item index="/teacher/score">
            <el-icon><EditPen /></el-icon>
            <span>作品评分</span>
          </el-menu-item>
          <el-menu-item index="/teacher/history">
            <el-icon><Reading /></el-icon>
            <span>评分记录</span>
          </el-menu-item>
          <el-menu-item index="/teacher/ranking">
            <el-icon><TrendCharts /></el-icon>
            <span>排行榜</span>
          </el-menu-item>
          <el-menu-item index="/teacher/notify">
            <el-icon><Bell /></el-icon>
            <span>消息通知</span>
          </el-menu-item>
        </el-menu>

        <div class="workspace-layout__section-note">
          深色主题下会切换到更接近夜间评审空间的阅读氛围，适合长时间查看材料与打分。
        </div>
      </aside>

      <div class="workspace-layout__main">
        <header class="workspace-layout__topbar">
          <div class="workspace-layout__headline">
            <h1>{{ pageTitle }}</h1>
            <p>{{ pageDescription }}</p>
          </div>

          <div class="workspace-layout__tools">
            <el-badge :value="unreadCount" :hidden="!hasUnread" class="workspace-layout__notify">
              <el-button circle @click="goNotify">
                <el-icon :size="18"><Bell /></el-icon>
              </el-button>
            </el-badge>
            <AppThemeToggle />
            <el-dropdown trigger="click">
              <span class="workspace-layout__user">
                <el-avatar :size="34" :src="userInfo?.avatar || undefined">
                  {{ avatarFallback }}
                </el-avatar>
                <span>{{ userInfo?.realName || '教师' }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/profile')">个人信息</el-dropdown-item>
                  <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </header>

        <main class="workspace-layout__content">
          <router-view />
        </main>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HomeFilled, Reading, EditPen, TrendCharts, Bell, ArrowDown } from '@element-plus/icons-vue'
import { getUnreadCount } from '@/api/notify'
import { useNotificationPolling } from '@/composables/useNotificationPolling'
import AppThemeToggle from '@/components/AppThemeToggle.vue'
import type { UserInfo } from '@/api/types'
import { clearAuthStorage, getCachedUserInfo } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const userInfo = ref<UserInfo | null>(getCachedUserInfo())
const { unreadCount, hasUnread } = useNotificationPolling({
  fetchFn: () => getUnreadCount('teacher').then((r) => r.data as { count: number }),
  eventName: 'teacher-notify-changed',
})
const avatarFallback = computed(() => userInfo.value?.realName?.charAt?.(0) || '师')
const syncUserInfo = () => {
  userInfo.value = getCachedUserInfo()
}

const descriptions: Record<string, string> = {
  '/teacher/dashboard': '在同一视图里掌握评审节奏、通知与近期工作。',
  '/teacher/score': '保持匿名评审，用材料与标准本身引导判断。',
  '/teacher/history': '回看评分记录，延续评审意见的一致性。',
  '/teacher/ranking': '从榜单理解整体质量分布与奖项层级。',
  '/teacher/notify': '集中查看系统通知与待处理提醒。',
}

onMounted(() => {
  syncUserInfo()
  window.addEventListener('focus', syncUserInfo)
  window.addEventListener('storage', syncUserInfo)
})

onUnmounted(() => {
  window.removeEventListener('focus', syncUserInfo)
  window.removeEventListener('storage', syncUserInfo)
})

const pageTitle = computed(() => {
  const map: Record<string, string> = {
    '/teacher/dashboard': '教师工作台',
    '/teacher/score': '作品评分',
    '/teacher/history': '我的评分记录',
    '/teacher/ranking': '排行榜',
    '/teacher/notify': '消息通知',
  }
  return (route.meta.title as string) || map[route.path] || '教师端'
})

const pageDescription = computed(() => descriptions[route.path] || '面向评审效率优化，但保留与公共展示一致的品牌秩序。')

const goNotify = () => router.push('/teacher/notify')

const logout = () => {
  clearAuthStorage()
  router.push('/login')
}
</script>

<style scoped>
.teacher-layout {
  --layout-accent: #6e3b46;
}
</style>
