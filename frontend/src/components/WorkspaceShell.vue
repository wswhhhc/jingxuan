<template>
  <div class="workspace-layout" :class="layoutClass">
    <div
      class="workspace-layout__frame"
      :class="{ 'workspace-layout__frame--collapsed': collapsible && isSidebarCollapsed }"
    >
      <aside
        class="workspace-layout__sidebar"
        :class="{ 'workspace-layout__sidebar--collapsed': collapsible && isSidebarCollapsed }"
      >
        <el-tooltip
          v-if="collapsible"
          :content="isSidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
          placement="right"
        >
          <el-button
            circle
            class="workspace-layout__collapse-toggle"
            @click="isSidebarCollapsed = !isSidebarCollapsed"
          >
            <el-icon :size="16">
              <Expand v-if="isSidebarCollapsed" />
              <Fold v-else />
            </el-icon>
          </el-button>
        </el-tooltip>

        <slot name="sidebar" :is-sidebar-collapsed="isSidebarCollapsed" />

        <slot name="sidebar-footer" />
      </aside>

      <div class="workspace-layout__main">
        <header class="workspace-layout__topbar">
          <div class="workspace-layout__headline">
            <h1>{{ title }}</h1>
            <p v-if="description">{{ description }}</p>
          </div>

          <div class="workspace-layout__tools">
            <el-badge
              :value="unreadCount"
              :hidden="!hasUnread"
              class="workspace-layout__notify"
            >
              <el-button circle @click="goNotify">
                <el-icon :size="18"><Bell /></el-icon>
              </el-button>
            </el-badge>
            <AppThemeToggle />
            <el-dropdown trigger="click" @command="handleDropdownCommand">
              <span class="workspace-layout__user">
                <el-avatar :size="34" :src="resolvedUserInfo?.avatar || undefined">
                  {{ avatarFallback }}
                </el-avatar>
                <span>{{ resolvedUserInfo?.realName || roleLabel }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <slot name="dropdown-items">
                    <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                    <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                  </slot>
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
import { useRouter } from 'vue-router'
import { Bell, ArrowDown, Expand, Fold } from '@element-plus/icons-vue'
import { getUnreadCount } from '@/api/v1/notify'
import { useNotificationPolling } from '@/composables/useNotificationPolling'
import AppThemeToggle from '@/components/AppThemeToggle.vue'
import type { UserInfo } from '@/api/types'
import { clearAuthStorage, getCachedUserInfo } from '@/utils/auth'

interface Props {
  title: string
  description?: string
  layoutClass?: string
  roleLabel?: string
  notifyPath?: string
  notifyEventName?: string
  collapsible?: boolean
  /** 可选：覆盖 userInfo（用于 student authStore 场景） */
  userInfo?: UserInfo | null
  /** 可选：自定义退出回调（用于 student authStore.logout 场景） */
  onLogout?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  description: '',
  layoutClass: '',
  roleLabel: '用户',
  notifyPath: '/notify',
  notifyEventName: undefined,
  collapsible: false,
  userInfo: undefined,
  onLogout: undefined,
})

const router = useRouter()
const isSidebarCollapsed = ref(false)
const localUserInfo = ref<UserInfo | null>(getCachedUserInfo())
const resolvedUserInfo = computed(() => (props.userInfo !== undefined ? props.userInfo : localUserInfo.value))

const eventName = computed(() => {
  if (props.notifyEventName) return props.notifyEventName
  const parts = props.notifyPath.split('/').filter(Boolean)
  return `${parts.join('-')}-notify-changed`
})

const { unreadCount, hasUnread } = useNotificationPolling({
  fetchFn: () => getUnreadCount().then((r) => r.data as { count: number }),
  eventName: eventName.value,
})

const avatarFallback = computed(
  () => resolvedUserInfo.value?.realName?.charAt?.(0) || props.roleLabel.charAt(0),
)

const syncUserInfo = () => {
  // only sync from cache when not using a prop-override
  if (props.userInfo === undefined) {
    localUserInfo.value = getCachedUserInfo()
  }
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

const goNotify = () => router.push(props.notifyPath)

function handleDropdownCommand(cmd: string) {
  if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'logout') handleLogout()
}

const handleLogout = () => {
  if (props.onLogout) {
    props.onLogout()
  } else {
    clearAuthStorage()
    router.push('/login')
  }
}

defineExpose({ isSidebarCollapsed })
</script>
