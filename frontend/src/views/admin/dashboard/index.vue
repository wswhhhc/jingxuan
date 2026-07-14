<template>
  <div class="workspace-page dashboard-page">
    <section class="workspace-section workspace-section--soft reveal-up">
      <div class="workspace-intro">
        <div class="workspace-intro__body">
          <span class="workspace-intro__eyebrow">Control Archive</span>
          <h2 class="workspace-intro__title">平台控制台</h2>
          <p class="workspace-intro__summary">
            用统一的档案式控制台查看作品总量、审核状态、评分分布与最近提交，让管理动作先建立全局判断。
          </p>
        </div>
      </div>
    </section>

    <section class="workspace-stats reveal-up reveal-delay-1">
      <article v-for="card in statCards" :key="card.label" class="workspace-stat">
        <span class="workspace-stat__eyebrow">Archive Data</span>
        <strong class="workspace-stat__value">{{ card.value }}</strong>
        <div class="workspace-stat__label">{{ card.label }}</div>
      </article>
    </section>

    <section class="dashboard-grid reveal-up reveal-delay-2">
      <article class="workspace-section">
        <div class="workspace-toolbar workspace-toolbar--tight">
          <div class="workspace-toolbar__body">
            <h3 class="workspace-toolbar__title">技术栈分布</h3>
            <p class="workspace-toolbar__desc">观察当前作品技术重心。</p>
          </div>
        </div>
        <div ref="techChartRef" class="chart-box" />
      </article>

      <article class="workspace-section">
        <div class="workspace-toolbar workspace-toolbar--tight">
          <div class="workspace-toolbar__body">
            <h3 class="workspace-toolbar__title">审核状态分布</h3>
            <p class="workspace-toolbar__desc">快速确认待审核、已通过与驳回比例。</p>
          </div>
        </div>
        <div ref="statusChartRef" class="chart-box" />
      </article>
    </section>

    <section class="workspace-section reveal-up reveal-delay-2">
      <div class="workspace-toolbar workspace-toolbar--tight">
        <div class="workspace-toolbar__body">
          <h3 class="workspace-toolbar__title">评分分布</h3>
          <p class="workspace-toolbar__desc">从分数段看整体质量与评审结果分层。</p>
        </div>
      </div>
      <div ref="scoreChartRef" class="chart-box chart-box--wide" />
    </section>

    <section class="workspace-split reveal-up reveal-delay-3">
      <article class="workspace-section">
        <div class="workspace-toolbar workspace-toolbar--tight">
          <div class="workspace-toolbar__body">
            <h3 class="workspace-toolbar__title">最近提交</h3>
            <p class="workspace-toolbar__desc">优先查看最近进入审核池的作品。</p>
          </div>
        </div>

        <el-table :data="recentWorks" size="small" v-loading="loading">
          <el-table-column prop="title" label="作品" min-width="160" show-overflow-tooltip />
          <el-table-column label="提交人" width="100">
            <template #default="{ row }">
              <span>{{ row.submitterName || '--' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="submitTime" label="时间" width="160" />
        </el-table>
      </article>

      <article class="workspace-section">
        <div class="workspace-toolbar workspace-toolbar--tight">
          <div class="workspace-toolbar__body">
            <h3 class="workspace-toolbar__title">快捷操作</h3>
            <p class="workspace-toolbar__desc">保留后台最常用的三类动作入口。</p>
          </div>
        </div>
        <div class="quick-actions">
          <el-button type="primary" @click="$router.push('/admin/audit')">审核作品</el-button>
          <el-button type="success" @click="$router.push('/admin/notice')">发布公告</el-button>
          <el-button type="warning" @click="$router.push('/admin/rules')">审核规则</el-button>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { init, use } from 'echarts/core'
import { PieChart, BarChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { LegacyGridContainLabel } from 'echarts/features'
import { CanvasRenderer } from 'echarts/renderers'
import type { ECharts } from 'echarts/core'
import { getDashboardStats, getDashboardCharts } from '@/api/admin/dashboard'
import type { DashboardStats, ChartData } from '@/api/admin/dashboard'
import { useThemeStore } from '@/stores/theme'

use([
  PieChart,
  BarChart,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  LegacyGridContainLabel,
  CanvasRenderer,
])

const loading = ref(false)
const themeStore = useThemeStore()
const recentWorks = ref<DashboardStats['recentWorks']>([])
const statCards = ref<{ label: string; value: number | string }[]>([
  { label: '作品总数', value: 0 },
  { label: '待审核', value: 0 },
  { label: '已发布', value: 0 },
  { label: '活跃批次', value: 0 },
])

const techChartRef = ref<HTMLElement | null>(null)
const statusChartRef = ref<HTMLElement | null>(null)
const scoreChartRef = ref<HTMLElement | null>(null)
let techChart: ECharts | null = null
let statusChart: ECharts | null = null
let scoreChart: ECharts | null = null
const latestChartData = ref<ChartData | null>(null)

const statusType = (s: number) => {
  const map: Record<number, string> = { 1: 'warning', 3: 'success', 2: 'danger', 0: 'info' }
  return map[s] || 'info'
}
const statusLabel = (s: number) => {
  const map: Record<number, string> = { 0: '草稿', 1: '待审核', 2: '已驳回', 3: '已通过' }
  return map[s] || '未知'
}

const loadStats = async () => {
  loading.value = true
  try {
    const res = await getDashboardStats()
    const data = res.data as DashboardStats
    statCards.value = [
      { label: '作品总数', value: data.totalWorks ?? '--' },
      { label: '待审核', value: data.pendingAudit ?? '--' },
      { label: '已发布', value: data.publishedWorks ?? '--' },
      { label: '活跃批次', value: data.activeBatches ?? '--' },
    ]
    recentWorks.value = data.recentWorks || []
  } catch {
    // backend unreachable — keep default zeros
  } finally {
    loading.value = false
  }
}

const loadCharts = async () => {
  try {
    const res = await getDashboardCharts()
    const data: ChartData = res.data
    latestChartData.value = data
    await nextTick()
    renderTechChart(data.techStackDist || [])
    renderStatusChart(data.statusDist || {})
    renderScoreChart(data.scoreDist || [])
  } catch {
    /* ignore */
  }
}

const readChartTokens = () => {
  const style = getComputedStyle(document.documentElement)
  return {
    text: style.getPropertyValue('--text-primary').trim(),
    textSecondary: style.getPropertyValue('--text-secondary').trim(),
    textMuted: style.getPropertyValue('--text-muted').trim(),
    border: style.getPropertyValue('--border-subtle').trim(),
    brand: style.getPropertyValue('--brand').trim(),
    accent: style.getPropertyValue('--accent').trim(),
    success: style.getPropertyValue('--success').trim(),
    warning: style.getPropertyValue('--warning').trim(),
    danger: style.getPropertyValue('--danger').trim(),
    info: style.getPropertyValue('--info').trim(),
    surface: style.getPropertyValue('--card-bg').trim(),
    surfaceStrong: style.getPropertyValue('--card-bg-strong').trim(),
    chartGlow: style.getPropertyValue('--chart-glow').trim(),
    paletteA: style.getPropertyValue('--chart-1').trim(),
    paletteB: style.getPropertyValue('--chart-2').trim(),
    paletteC: style.getPropertyValue('--chart-3').trim(),
    paletteD: style.getPropertyValue('--chart-4').trim(),
    paletteE: style.getPropertyValue('--chart-5').trim(),
  }
}

const renderTechChart = (data: { name: string; value: number }[]) => {
  if (!techChartRef.value) return
  if (!techChart) techChart = init(techChartRef.value)
  const tokens = readChartTokens()
  techChart.setOption({
    color: [tokens.paletteA, tokens.paletteB, tokens.paletteC, tokens.paletteD, tokens.paletteE],
    tooltip: {
      trigger: 'item',
      backgroundColor: tokens.surfaceStrong,
      borderColor: tokens.border,
      textStyle: { color: tokens.text },
    },
    series: [
      {
        type: 'pie',
        radius: ['48%', '74%'],
        center: ['50%', '54%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 10,
          borderColor: tokens.surface,
          borderWidth: 4,
          shadowBlur: 24,
          shadowColor: tokens.chartGlow,
        },
        label: {
          formatter: '{b}\n{c} 项',
          color: tokens.textSecondary,
        },
        labelLine: {
          lineStyle: { color: tokens.border },
          length: 14,
          length2: 10,
        },
        emphasis: {
          scale: true,
          scaleSize: 8,
        },
        data,
      },
    ],
  })
}

const renderStatusChart = (data: Record<string, number>) => {
  if (!statusChartRef.value) return
  if (!statusChart) statusChart = init(statusChartRef.value)
  const tokens = readChartTokens()
  const items = Object.entries(data || {}).map(([name, value]) => ({ name, value }))
  statusChart.setOption({
    color: [tokens.warning, tokens.success, tokens.danger, tokens.info, tokens.accent],
    tooltip: {
      trigger: 'item',
      backgroundColor: tokens.surfaceStrong,
      borderColor: tokens.border,
      textStyle: { color: tokens.text },
    },
    legend: {
      orient: 'vertical',
      right: 2,
      top: 'center',
      itemWidth: 14,
      itemHeight: 14,
      itemGap: 14,
      textStyle: {
        color: tokens.textSecondary,
        fontSize: 14,
      },
    },
    series: [
      {
        type: 'pie',
        center: ['36%', '56%'],
        radius: ['42%', '68%'],
        itemStyle: {
          borderRadius: 10,
          borderColor: tokens.surface,
          borderWidth: 4,
          shadowBlur: 20,
          shadowColor: tokens.chartGlow,
        },
        label: {
          formatter: '{b}: {c}',
          color: tokens.textSecondary,
        },
        labelLine: {
          lineStyle: { color: tokens.border },
        },
        data: items,
      },
    ],
  })
}

const renderScoreChart = (data: { name: string; value: number }[]) => {
  if (!scoreChartRef.value) return
  if (!scoreChart) scoreChart = init(scoreChartRef.value)
  const tokens = readChartTokens()
  const scoreOrder = ['60以下', '60-69', '70-79', '80-89', '90-100']
  const sorted = scoreOrder.map((name) => {
    const item = (data || []).find((d) => d.name === name)
    return { name, value: item?.value || 0 }
  })
  scoreChart.setOption({
    color: [tokens.paletteA],
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
        shadowStyle: {
          color: tokens.chartGlow,
        },
      },
      backgroundColor: tokens.surfaceStrong,
      borderColor: tokens.border,
      textStyle: { color: tokens.text },
    },
    grid: {
      left: 24,
      right: 16,
      top: 28,
      bottom: 24,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: sorted.map((s) => s.name),
      axisLine: { lineStyle: { color: tokens.border } },
      axisTick: { show: false },
      axisLabel: { color: tokens.textSecondary },
    },
    yAxis: {
      type: 'value',
      name: '作品数',
      nameTextStyle: { color: tokens.textMuted },
      splitLine: { lineStyle: { color: tokens.border, type: 'dashed' } },
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: tokens.textSecondary },
    },
    series: [
      {
        type: 'bar',
        data: sorted.map((s) => s.value),
        barWidth: 26,
        roundCap: true,
        showBackground: true,
        backgroundStyle: {
          color: tokens.chartGlow,
          borderRadius: [12, 12, 0, 0],
        },
        itemStyle: {
          color: tokens.paletteA,
          borderRadius: [12, 12, 0, 0],
          shadowBlur: 18,
          shadowColor: tokens.chartGlow,
        },
      },
    ],
  })
}

const handleResize = () => {
  techChart?.resize()
  statusChart?.resize()
  scoreChart?.resize()
}

onMounted(() => {
  loadStats()
  loadCharts()
  window.addEventListener('resize', handleResize)
})

watch(
  () => themeStore.mode,
  async () => {
    await nextTick()
    const data = latestChartData.value
    if (!data) return
    renderTechChart(data.techStackDist || [])
    renderStatusChart(data.statusDist || {})
    renderScoreChart(data.scoreDist || [])
  },
)

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  techChart?.dispose()
  statusChart?.dispose()
  scoreChart?.dispose()
})
</script>

<style scoped>
.dashboard-page {
  max-width: 1240px;
  margin: 0 auto;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.chart-box {
  position: relative;
  height: 300px;
  margin-top: 10px;
  border-radius: 22px;
  background:
    radial-gradient(circle at top left, var(--chart-glow), transparent 42%),
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--card-bg) 92%, transparent),
      color-mix(in srgb, var(--card-bg-strong) 96%, transparent)
    );
  border: 1px solid color-mix(in srgb, var(--border-subtle) 82%, transparent);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2);
  overflow: hidden;
}

.chart-box::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.12), transparent 30%),
    linear-gradient(0deg, transparent, rgba(255, 255, 255, 0.04));
  pointer-events: none;
}

.chart-box--wide {
  height: 320px;
}

:global([data-theme='dark']) .chart-box {
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.04),
    0 18px 36px rgba(0, 0, 0, 0.18);
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

@media (max-width: 900px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
