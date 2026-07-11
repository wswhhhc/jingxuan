<template>
  <div class="workspace-page dashboard-page" v-loading="loading">
    <section class="workspace-section workspace-section--soft reveal-up">
      <div class="workspace-intro">
        <div class="workspace-intro__body">
          <span class="workspace-intro__eyebrow">Review Chamber</span>
          <h2 class="workspace-intro__title">教师工作台</h2>
          <p class="workspace-intro__summary">
            先在总览席确认评审节奏、完成率与通知状态，再进入具体作品和历史记录，避免直接扎进单条评分里失去全局感。
          </p>
        </div>
        <div class="hero-progress">
          <span class="hero-progress__label">完成率</span>
          <strong class="hero-progress__value">{{ completionRate }}%</strong>
          <el-progress :percentage="completionRate" :stroke-width="10" />
        </div>
      </div>
    </section>

    <section class="workspace-stats reveal-up reveal-delay-1">
      <article v-for="card in statCards" :key="card.label" class="workspace-stat">
        <span class="workspace-stat__eyebrow">Review Data</span>
        <strong class="workspace-stat__value">{{ card.value }}</strong>
        <div class="workspace-stat__label">{{ card.label }}</div>
        <div class="workspace-stat__note">{{ card.tip }}</div>
      </article>
    </section>

    <section class="workspace-split reveal-up reveal-delay-2">
      <article class="workspace-section">
        <div class="workspace-toolbar workspace-toolbar--tight">
          <div class="workspace-toolbar__body">
            <h3 class="workspace-toolbar__title">工作概览</h3>
            <p class="workspace-toolbar__desc">把待评分、已完成、总量与未读消息放在同一张评审简报里。</p>
          </div>
        </div>

        <div class="workspace-kpi-grid">
          <div class="workspace-kpi">
            <span class="workspace-kpi__label">当前待评分作品</span>
            <strong class="workspace-kpi__value">{{ stats.pendingWorks }}</strong>
          </div>
          <div class="workspace-kpi">
            <span class="workspace-kpi__label">已完成评分作品</span>
            <strong class="workspace-kpi__value">{{ stats.scoredWorks }}</strong>
          </div>
          <div class="workspace-kpi">
            <span class="workspace-kpi__label">总可评分作品</span>
            <strong class="workspace-kpi__value">{{ totalScorable }}</strong>
          </div>
          <div class="workspace-kpi">
            <span class="workspace-kpi__label">当前未读通知</span>
            <strong class="workspace-kpi__value">{{ stats.unreadCount }}</strong>
          </div>
        </div>
      </article>

      <article class="workspace-section">
        <div class="workspace-toolbar workspace-toolbar--tight">
          <div class="workspace-toolbar__body">
            <h3 class="workspace-toolbar__title">快捷入口</h3>
            <p class="workspace-toolbar__desc">保留最常用的四个动作，减少在菜单中来回跳转。</p>
          </div>
        </div>

        <div class="quick-actions">
          <el-button type="primary" @click="router.push('/teacher/score')">进入评分区</el-button>
          <el-button @click="router.push('/teacher/history')">查看评分记录</el-button>
          <el-button @click="router.push('/teacher/notify')">处理消息通知</el-button>
          <el-button @click="router.push('/teacher/ranking')">查看排行榜</el-button>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getTeacherDashboardStats } from '@/api/teacher/dashboard'

const router = useRouter()
const loading = ref(false)

const stats = reactive({
  pendingWorks: 0,
  scoredWorks: 0,
  totalScorableWorks: 0,
  completionRate: 0,
  activeBatchCount: 0,
  unreadCount: 0,
})

const totalScorable = computed(() => stats.totalScorableWorks || stats.pendingWorks + stats.scoredWorks)
const completionRate = computed(() => stats.completionRate)

const statCards = computed(() => [
  { label: '待评分作品数', value: stats.pendingWorks, tip: '当前筛出仍未完成评分的作品' },
  { label: '已评分作品数', value: stats.scoredWorks, tip: '已提交或更新过评分的作品数量' },
  { label: '可用评分批次数', value: stats.activeBatchCount, tip: '状态为进行中的评分批次' },
  { label: '未读通知数', value: stats.unreadCount, tip: '待查看的系统通知消息' },
  { label: '评分完成率', value: `${completionRate.value}%`, tip: '已评分 / 总可评分作品' },
])

const loadStats = async () => {
  loading.value = true
  try {
    const res = await getTeacherDashboardStats()
    const data = res.data
    stats.pendingWorks = data?.pendingWorks || 0
    stats.scoredWorks = data?.scoredWorks || 0
    stats.totalScorableWorks = data?.totalScorableWorks || 0
    stats.completionRate = data?.completionRate || 0
    stats.activeBatchCount = data?.activeBatchCount || 0
    stats.unreadCount = data?.unreadCount || 0
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<style scoped>
.dashboard-page {
  max-width: 1240px;
  margin: 0 auto;
}

.hero-progress {
  width: 280px;
  max-width: 100%;
  padding: 18px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--card-bg) 80%, transparent);
}

.hero-progress__label {
  font-size: 12px;
  color: var(--text-muted);
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.hero-progress__value {
  display: block;
  margin: 10px 0 14px;
  font-family: var(--font-display);
  font-size: 34px;
  line-height: 1;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

@media (max-width: 900px) {
  .hero-progress {
    width: 100%;
  }
}
</style>
