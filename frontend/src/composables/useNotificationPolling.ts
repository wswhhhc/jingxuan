import { ref, computed, onMounted, onUnmounted } from 'vue'

interface UseNotificationPollingOptions {
  /** 获取未读数的 API 函数 */
  fetchFn: () => Promise<{ count: number }>
  /** 自定义事件名称（用于通知页面标记已读后触发刷新） */
  eventName: string
  /** 轮询间隔（毫秒，默认 30s） */
  intervalMs?: number
}

/**
 * 通知轮询 composable
 * 统一管理未读数轮询 + 自定义事件侦听，消除三个 Layout 中的重复代码
 */
export function useNotificationPolling(options: UseNotificationPollingOptions) {
  const { fetchFn, eventName, intervalMs = 30000 } = options

  const unreadCount = ref(0)
  const hasUnread = computed(() => unreadCount.value > 0)

  let timer: ReturnType<typeof setInterval> | null = null

  const fetchUnread = async () => {
    try {
      const res = await fetchFn()
      unreadCount.value = Number(res?.count ?? 0)
    } catch {
      unreadCount.value = 0
    }
  }

  const startPolling = () => {
    fetchUnread()
    timer = setInterval(fetchUnread, intervalMs)
    window.addEventListener(eventName, fetchUnread)
  }

  const stopPolling = () => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    window.removeEventListener(eventName, fetchUnread)
  }

  onMounted(startPolling)
  onUnmounted(stopPolling)

  return { unreadCount, hasUnread, fetchUnread }
}
