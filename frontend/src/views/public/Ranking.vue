<template>
  <div class="app-page ranking-page">
    <section class="surface-panel hero-grid reveal-up">
      <div>
        <span class="page-kicker">Ranking & Awards</span>
        <h1 class="page-title">作品排行榜</h1>
        <p class="page-summary">
          以综合得分为主线，同时保留创新性、技术难度、完成度与实用性的维度线索。榜单既是结果公布，也是一种公开的评价结构。
        </p>
      </div>
      <div class="hero-metrics">
        <div class="hero-metric">
          <span class="hero-metric__label">Works</span>
          <span class="hero-metric__value">{{ list.length }}</span>
        </div>
        <div class="hero-metric">
          <span class="hero-metric__label">Batches</span>
          <span class="hero-metric__value">{{ batches.length }}</span>
        </div>
        <div class="hero-metric">
          <span class="hero-metric__label">Categories</span>
          <span class="hero-metric__value">{{ categories.length }}</span>
        </div>
        <div class="hero-metric">
          <span class="hero-metric__label">Top List</span>
          <span class="hero-metric__value">50</span>
        </div>
      </div>
    </section>

    <section class="filter-panel reveal-up reveal-delay-1">
      <div class="section-heading">
        <div>
          <h2 class="section-heading__title">榜单筛选</h2>
          <p class="section-heading__meta">按批次与技术分类切换查看不同作品群组。</p>
        </div>
      </div>

      <el-form :model="query" inline class="ranking-filters">
        <el-form-item label="批次">
          <el-select
            v-model="query.batchId"
            placeholder="选择批次"
            clearable
            style="width: 240px"
            @change="onBatchChange"
          >
            <el-option v-for="b in batches" :key="b.batchId" :label="b.batchName" :value="b.batchId" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.category" placeholder="全部" clearable style="width: 160px" @change="loadList">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadList">更新榜单</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-panel reveal-up reveal-delay-2">
      <div class="section-heading">
        <div>
          <h2 class="section-heading__title">综合榜单</h2>
          <p class="section-heading__meta">取所有评分教师的平均分，按总分排序。</p>
        </div>
      </div>

      <div v-if="!query.batchId && !loading" class="workspace-empty">
        <el-empty description="请选择批次查看排行" />
      </div>

      <el-table v-else :data="list" v-loading="loading">
        <el-table-column label="排名" width="86">
          <template #default="{ row }">
            <span v-if="row.rankNo <= 3" class="rank-medal">{{ ['🥇', '🥈', '🥉'][row.rankNo - 1] }}</span>
            <span v-else class="rank-num">{{ row.rankNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="作品名称" min-width="220">
          <template #default="{ row }">
            <router-link :to="`/works/${row.workId}`" class="work-link">{{ row.workTitle }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="techStack" label="技术栈" width="160" show-overflow-tooltip />
        <el-table-column label="各维度得分" min-width="260">
          <template #default="{ row }">
            <div class="dim-scores">
              <span title="创新性">创 {{ row.avgInnovation }}</span>
              <span title="技术难度">技 {{ row.avgDifficulty }}</span>
              <span title="完成度">完 {{ row.avgCompletion }}</span>
              <span title="实用性">实 {{ row.avgPracticality }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="综合得分" width="110">
          <template #default="{ row }">
            <span class="total-score">{{ row.avgScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="获奖情况" min-width="180">
          <template #default="{ row }">
            <div v-if="row.rewardLevel" class="award-cell">
              <el-tag :type="rewardTagType(row.rewardLevel)" size="small">{{ row.rewardLevel }}</el-tag>
              <span class="prize-name">{{ row.prizeName || row.rewardLevel }}</span>
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="list.length === 0 && !loading && query.batchId" class="table-empty">暂无排行数据</div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getRankingList, getRankingBatches, getRankingCategories } from '@/api/public/ranking'
import type { RankItem } from '@/api/types'

import { rewardTagType } from '@/utils/format'
const loading = ref(false)
const list = ref<RankItem[]>([])
const batches = ref<{ batchId: number; batchName: string }[]>([])
const categories = ref<string[]>([])

const query = reactive({
  batchId: undefined as number | undefined,
  category: undefined as string | undefined,
})

const loadBatches = async () => {
  try {
    const res = await getRankingBatches()
    batches.value = res.data || []
  } catch {
    batches.value = []
  }
}

const loadCategories = async () => {
  try {
    const res = await getRankingCategories(query.batchId)
    categories.value = res.data || []
  } catch {
    categories.value = []
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await getRankingList({ batchId: query.batchId, topN: 50, techStack: query.category || undefined })
    list.value = (res.data as RankItem[]) || []
  } finally {
    loading.value = false
  }
}

// 批次切换时重新加载分类和榜单
const onBatchChange = (val: number | undefined) => {
  query.batchId = val
  query.category = undefined
  loadCategories()
  loadList()
}

onMounted(() => {
  loadBatches()
  loadCategories()
  loadList()
})
</script>

<style scoped>
.ranking-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.ranking-filters :deep(.el-form-item) {
  margin-bottom: 0;
}

.rank-medal {
  font-size: 24px;
}

.rank-num {
  font-family: var(--font-display);
  font-size: 22px;
  color: var(--text-primary);
}

.total-score {
  font-family: var(--font-display);
  font-size: 24px;
  color: var(--brand);
}

.dim-scores {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.dim-scores span {
  padding: 6px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--page-bg) 62%, transparent);
  color: var(--text-secondary);
  font-size: 12px;
}

.work-link {
  color: var(--text-primary);
  border-bottom: 1px solid transparent;
  transition:
    color var(--transition-fast),
    border-color var(--transition-fast);
}

.work-link:hover {
  color: var(--brand);
  border-color: color-mix(in srgb, var(--brand) 42%, transparent);
}

.award-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.prize-name {
  color: var(--text-secondary);
}

.table-empty {
  padding: 28px 0 8px;
  text-align: center;
  color: var(--text-muted);
}
</style>
