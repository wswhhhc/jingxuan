<template>
  <div class="workspace-page notify-page">
    <section class="workspace-section workspace-section--soft reveal-up">
      <div class="workspace-toolbar">
        <div class="workspace-toolbar__body">
          <h2 class="workspace-toolbar__title">消息通知</h2>
          <p class="workspace-toolbar__desc">{{ description }}</p>
        </div>
        <div class="workspace-toolbar__actions">
          <el-checkbox v-model="unreadOnly" label="仅看未读" @change="loadList" />
          <el-button size="small" @click="handleMarkAll">全部已读</el-button>
          <el-button size="small" type="danger" plain @click="handleDeleteRead">删除已读</el-button>
        </div>
      </div>
    </section>

    <section class="workspace-section reveal-up reveal-delay-1">
      <div v-loading="loading">
        <div v-if="list.length > 0" class="workspace-collection">
          <article
            v-for="item in list"
            :key="item.id"
            class="notify-item"
            :class="{ unread: !item.isRead }"
            @click="handleRead(item)"
          >
            <div class="notify-header">
              <div class="notify-title-wrap">
                <span class="notify-title">{{ item.title }}</span>
                <span v-if="!item.isRead" class="unread-dot" />
              </div>
              <span class="notify-time">{{ item.createTime }}</span>
            </div>
            <div class="notify-content" :class="{ 'notify-content--long': isContentLong(item.content) }">
              {{ item.content }}
            </div>
            <span v-if="isContentLong(item.content)" class="notify-expand" @click.stop="showDetail(item)"
              >查看详情 →</span
            >
          </article>
        </div>

        <div v-else-if="!loading" class="workspace-empty">
          <el-empty description="暂无通知" />
        </div>
      </div>

      <div class="workspace-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          size="small"
          @change="loadList"
        />
      </div>
    </section>

    <!-- 通知详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="detailItem?.title || '通知详情'" width="600px">
      <div class="notify-detail-meta">
        <span>{{ detailItem?.createTime }}</span>
        <el-tag v-if="detailItem && !detailItem.isRead" size="small" type="danger">未读</el-tag>
      </div>
      <div class="notify-detail-content">{{ detailItem?.content }}</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

interface NotifyItem {
  id: number
  title: string
  content: string
  isRead: number
  createTime: string
}

interface NotifyApi {
  getNotifyList(params: { page?: number; size?: number; unreadOnly?: boolean }): Promise<{
    data?: NotifyItem[] | { records?: NotifyItem[]; total?: number }
  }>
  markAsRead(id: number): Promise<unknown>
  markAllRead(): Promise<unknown>
  deleteRead?(): Promise<unknown>
}

const props = defineProps<{
  api: NotifyApi
  eventName: string
  description?: string
}>()

const loading = ref(false)
const list = ref<NotifyItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const unreadOnly = ref(false)
const detailVisible = ref(false)
const detailItem = ref<NotifyItem | null>(null)

const isContentLong = (content: string) => content && content.length > 120

const showDetail = (item: NotifyItem) => {
  detailItem.value = item
  detailVisible.value = true
}

const emitNotifyChanged = () => {
  window.dispatchEvent(new CustomEvent(props.eventName))
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await props.api.getNotifyList({
      page: page.value,
      size: size.value,
      unreadOnly: unreadOnly.value || undefined,
    })
    if (Array.isArray(res.data)) {
      list.value = res.data
      total.value = res.data.length
    } else {
      list.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } finally {
    loading.value = false
  }
}

const handleRead = async (item: NotifyItem) => {
  if (!item.isRead) {
    try {
      await props.api.markAsRead(item.id)
      item.isRead = 1
      emitNotifyChanged()
    } catch (e) {
      console.error('标记已读失败:', e)
    }
  }
}

const handleMarkAll = async () => {
  try {
    await props.api.markAllRead()
    list.value.forEach((item) => {
      item.isRead = 1
    })
    emitNotifyChanged()
    ElMessage.success('全部已读')
  } catch (e) {
    console.error('全部已读操作失败:', e)
    ElMessage.error('操作失败，请重试')
  }
}

const handleDeleteRead = async () => {
  if (!props.api.deleteRead) return
  try {
    await ElMessageBox.confirm('确认删除所有已读通知？', '提示')
    await props.api.deleteRead()
    ElMessage.success('已删除')
    loadList()
    emitNotifyChanged()
  } catch {
    /* 取消或错误 */
  }
}

onMounted(loadList)
</script>

<style scoped>
.notify-page {
  max-width: 1100px;
  margin: 0 auto;
}

.notify-item {
  padding: 18px 20px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--card-bg) 82%, transparent);
  cursor: pointer;
  transition:
    transform var(--transition-base),
    border-color var(--transition-fast),
    background-color var(--transition-fast);
}

.notify-item:hover {
  transform: translateY(-1px);
  border-color: var(--border-color);
}

.notify-item.unread {
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--brand-soft) 70%, transparent), transparent 30%),
    color-mix(in srgb, var(--card-bg) 82%, transparent);
}

.notify-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.notify-title-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.notify-title {
  font-weight: 600;
  font-size: 15px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--brand);
  flex-shrink: 0;
}

.notify-time {
  color: var(--text-muted);
  font-size: 12px;
}

.notify-content {
  margin-top: 10px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.8;
}

.notify-content--long {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notify-expand {
  display: inline-block;
  margin-top: 6px;
  color: var(--brand);
  font-size: 12px;
  cursor: pointer;
  transition: opacity var(--transition-fast);
}

.notify-expand:hover {
  opacity: 0.7;
}

.notify-detail-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  color: var(--text-muted);
  font-size: 13px;
}

.notify-detail-content {
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.9;
  white-space: pre-wrap;
}
</style>
