<template>
  <div class="workspace-page todo-page">
    <section class="workspace-section workspace-section--soft reveal-up">
      <div class="workspace-intro">
        <div class="workspace-intro__body">
          <span class="workspace-intro__eyebrow">Task Board</span>
          <h2 class="workspace-intro__title">我的待办</h2>
          <p class="workspace-intro__summary">查看和管理各评分批次的待办任务，点击「去提交」上传你的作品。</p>
        </div>
      </div>
    </section>

    <section class="workspace-section reveal-up reveal-delay-1">
      <div class="workspace-toolbar workspace-toolbar--tight">
        <div class="workspace-toolbar__body">
          <h3 class="workspace-toolbar__title">待办列表</h3>
          <p class="workspace-toolbar__desc">共 {{ tasks.length }} 项待办</p>
        </div>
      </div>

      <div v-if="loading" v-loading="loading" class="workspace-loading" style="min-height: 200px" />

      <template v-else>
        <div v-if="tasks.length === 0" class="workspace-empty">
          <el-empty description="暂无待办任务" :image-size="72" />
        </div>

        <div v-else class="todo-list">
          <div
            v-for="task in tasks"
            :key="task.id"
            class="todo-card"
            :class="['todo-card--' + statusClass(task.status)]"
          >
            <div class="todo-card__left">
              <div class="todo-card__icon">
                <el-icon :size="22">
                  <component :is="statusIcon(task.status)" />
                </el-icon>
              </div>
            </div>

            <div class="todo-card__body">
              <div class="todo-card__header">
                <h4 class="todo-card__title">{{ task.title || task.batchName || `批次 #${task.batchId}` }}</h4>
                <el-tag :type="statusTag(task.status)" size="small" effect="dark">
                  {{ statusLabel(task.status) }}
                </el-tag>
              </div>

              <p v-if="task.content" class="todo-card__desc">{{ task.content }}</p>

              <div class="todo-card__meta">
                <span v-if="task.startTime || task.endTime" class="todo-card__time">
                  <el-icon :size="14"><Calendar /></el-icon>
                  {{ task.startTime?.replace('T', ' ') }} ~ {{ task.endTime?.replace('T', ' ') }}
                </span>
                <span class="todo-card__created">创建于 {{ task.createTime?.replace('T', ' ') }}</span>
              </div>
            </div>

            <div class="todo-card__actions">
              <el-button
                v-if="task.status === 0 || task.status === 2"
                type="primary"
                size="small"
                @click="goSubmit(task)"
              >
                去提交
              </el-button>
              <el-button v-else-if="task.status === 1 && task.workId" size="small" @click="goViewWork(task)">
                查看作品
              </el-button>
            </div>
          </div>
        </div>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, type Component } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, CircleCheck, CircleClose, Clock, InfoFilled } from '@element-plus/icons-vue'
import { getMyTasks, type StudentTask } from '@/api/student/task'

const router = useRouter()
const loading = ref(false)
const tasks = ref<StudentTask[]>([])

function statusClass(status: number): string {
  const map: Record<number, string> = { 0: 'pending', 1: 'done', 2: 'rejected', 3: 'expired' }
  return map[status] || 'pending'
}

function statusIcon(status: number): Component {
  const map: Record<number, Component> = { 0: InfoFilled, 1: CircleCheck, 2: CircleClose, 3: Clock }
  return map[status] || InfoFilled
}

function statusTag(status: number): string {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }
  return map[status] || 'info'
}

function statusLabel(status: number): string {
  const map: Record<number, string> = { 0: '待处理', 1: '已完成', 2: '已驳回', 3: '已截止' }
  return map[status] || '未知'
}

async function loadTasks() {
  loading.value = true
  try {
    const res = await getMyTasks()
    const rawTasks: StudentTask[] = res.data || []

    // 补充过期判断
    rawTasks.forEach((task) => {
      if (task.endTime) {
        const now = new Date()
        const end = new Date(task.endTime.replace('T', ' '))
        if (end < now && task.status === 0) {
          task.status = 3
        }
      }
    })

    tasks.value = rawTasks
  } catch {
    ElMessage.error('加载待办失败')
    tasks.value = []
  } finally {
    loading.value = false
  }
}

function goSubmit(task: StudentTask) {
  router.push({
    path: '/student/works/create',
    query: { batchId: String(task.batchId), taskId: String(task.id) },
  })
}

function goViewWork(task: StudentTask) {
  if (task.workId) {
    router.push(`/student/works/view/${task.workId}`)
  }
}

onMounted(loadTasks)
</script>

<style scoped>
.todo-page {
  max-width: 940px;
  margin: 0 auto;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.todo-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 18px 20px;
  border-radius: var(--radius-sm, 8px);
  border: 1px solid var(--border-subtle, #e4e7ed);
  background: var(--card-bg, #fff);
  transition: box-shadow 0.2s;
}

.todo-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.todo-card--done {
  opacity: 0.72;
  border-left: 3px solid var(--el-color-success, #67c23a);
}

.todo-card--pending {
  border-left: 3px solid var(--el-color-warning, #e6a23c);
}

.todo-card--rejected {
  border-left: 3px solid var(--el-color-danger, #f56c6c);
}

.todo-card--expired {
  opacity: 0.5;
  border-left: 3px solid var(--el-color-info, #909399);
}

.todo-card__left {
  flex-shrink: 0;
  padding-top: 2px;
}

.todo-card--done .todo-card__icon {
  color: var(--el-color-success, #67c23a);
}
.todo-card--pending .todo-card__icon {
  color: var(--el-color-warning, #e6a23c);
}
.todo-card--rejected .todo-card__icon {
  color: var(--el-color-danger, #f56c6c);
}
.todo-card--expired .todo-card__icon {
  color: var(--el-color-info, #909399);
}

.todo-card__body {
  flex: 1;
  min-width: 0;
}

.todo-card__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.todo-card__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-card__desc {
  margin: 4px 0 8px;
  font-size: 13px;
  color: var(--text-secondary, #606266);
  line-height: 1.6;
  white-space: pre-wrap;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.todo-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: var(--text-muted, #909399);
}

.todo-card__time {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.todo-card__actions {
  flex-shrink: 0;
  padding-top: 2px;
}

.workspace-loading {
  display: flex;
  align-items: center;
  justify-content: center;
}

.workspace-empty {
  padding: 60px 0;
  text-align: center;
}
</style>
