<template>
  <div class="workspace-layout admin-layout">
    <div class="workspace-layout__frame" :class="{ 'workspace-layout__frame--collapsed': isSidebarCollapsed }">
      <aside class="workspace-layout__sidebar" :class="{ 'workspace-layout__sidebar--collapsed': isSidebarCollapsed }">
        <el-tooltip :content="isSidebarCollapsed ? '展开侧边栏' : '收起侧边栏'" placement="right">
          <el-button circle class="workspace-layout__collapse-toggle" @click="isSidebarCollapsed = !isSidebarCollapsed">
            <el-icon :size="16">
              <Expand v-if="isSidebarCollapsed" />
              <Fold v-else />
            </el-icon>
          </el-button>
        </el-tooltip>

        <div class="workspace-layout__brand">
          <span v-show="!isSidebarCollapsed" class="workspace-layout__brand-note">Control Archive</span>
          <span v-show="!isSidebarCollapsed" class="workspace-layout__brand-title">管理后台</span>
        </div>

        <el-menu
          :default-active="route.path"
          :collapse="isSidebarCollapsed"
          :collapse-transition="false"
          router
          class="workspace-layout__menu"
        >
          <el-menu-item v-for="item in adminMenuItems" :key="item.index" :index="item.index">
            <el-tooltip :content="item.label" :disabled="!isSidebarCollapsed" placement="right">
              <div class="workspace-layout__menu-item">
                <el-icon><component :is="item.icon" /></el-icon>
                <span v-show="!isSidebarCollapsed">{{ item.label }}</span>
              </div>
            </el-tooltip>
          </el-menu-item>
        </el-menu>
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
                <span>{{ userInfo?.realName || '管理员' }}</span>
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
import {
  Odometer,
  Finished,
  Notification,
  Setting,
  TrophyBase,
  Timer,
  Key,
  Bell,
  ArrowDown,
  User,
  Document,
  List,
  ChatDotRound,
  Fold,
  Expand,
} from '@element-plus/icons-vue'
import { getUnreadCount } from '@/api/notify'
import { useNotificationPolling } from '@/composables/useNotificationPolling'
import AppThemeToggle from '@/components/AppThemeToggle.vue'
import type { UserInfo } from '@/api/types'
import { clearAuthStorage, getCachedUserInfo } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const isSidebarCollapsed = ref(false)
const userInfo = ref<UserInfo | null>(getCachedUserInfo())
const { unreadCount, hasUnread } = useNotificationPolling({
  fetchFn: () => getUnreadCount('admin').then((r) => r.data as { count: number }),
  eventName: 'admin-notify-changed',
})
const avatarFallback = computed(() => userInfo.value?.realName?.charAt?.(0) || '管')
const syncUserInfo = () => {
  userInfo.value = getCachedUserInfo()
}

const descriptions: Record<string, string> = {
  '/admin/dashboard': '用更强的层次和图表节奏总览平台状态。',
  '/admin/audit': '围绕审核流程组织筛选、详情和发布动作。',
  '/admin/notice': '用统一的编辑界面维护公告内容。',
  '/admin/comment': '治理评论秩序，同时保留公开交流的可读性。',
  '/admin/rules': '以结构化方式维护审核规则。',
  '/admin/prize': '整理奖项、奖品与榜单呈现逻辑。',
  '/admin/score-batch': '配置评审批次，控制时间与范围。',
  '/admin/roles': '管理角色权限，保持后台秩序清晰。',
  '/admin/users': '集中维护用户与账号状态。',
  '/admin/log': '查看系统操作脉络，保留审计线索。',
  '/admin/dict': '维护字典数据，让配置与内容彼此一致。',
}

const adminMenuItems = [
  { index: '/admin/dashboard', label: '控制台', icon: Odometer },
  { index: '/admin/audit', label: '审核管理', icon: Finished },
  { index: '/admin/notice', label: '公告管理', icon: Notification },
  { index: '/admin/comment', label: '评论管理', icon: ChatDotRound },
  { index: '/admin/rules', label: '审核规则', icon: Setting },
  { index: '/admin/prize', label: '奖品配置', icon: TrophyBase },
  { index: '/admin/score-batch', label: '评分批次', icon: Timer },
  { index: '/admin/roles', label: '角色权限', icon: Key },
  { index: '/admin/users', label: '用户管理', icon: User },
  { index: '/admin/log', label: '操作日志', icon: Document },
  { index: '/admin/dict', label: '数据字典', icon: List },
]

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
    '/admin/dashboard': '控制台',
    '/admin/audit': '审核管理',
    '/admin/notice': '公告管理',
    '/admin/comment': '评论管理',
    '/admin/rules': '审核规则',
    '/admin/prize': '奖品配置',
    '/admin/score-batch': '评分批次',
    '/admin/roles': '角色权限',
    '/admin/users': '用户管理',
    '/admin/notify': '消息通知',
    '/admin/log': '操作日志',
    '/admin/dict': '数据字典',
  }
  return map[route.path] || '控制台'
})

const pageDescription = computed(() => descriptions[route.path] || '后台保持理性与秩序，但不再保留默认 SaaS 模板气质。')

const goNotify = () => router.push('/admin/notify')

const logout = () => {
  clearAuthStorage()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  --layout-accent: #7f2436;
}
</style>
