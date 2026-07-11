<template>
  <div class="workspace-page my-ranking-page">
    <section class="workspace-section workspace-section--soft reveal-up">
      <div class="workspace-intro">
        <div class="workspace-intro__body">
          <span class="workspace-intro__eyebrow">Score Overview</span>
          <h2 class="workspace-intro__title">我的评分与排名</h2>
          <p class="workspace-intro__summary">
            用统一的评审视图回看每个批次的平均分、维度表现与排名位置，方便继续理解作品在整体中的位置。
          </p>
        </div>
      </div>
    </section>

    <section class="workspace-stats reveal-up reveal-delay-1">
      <article class="workspace-stat">
        <span class="workspace-stat__eyebrow">Batches</span>
        <strong class="workspace-stat__value">{{ rankList.length }}</strong>
        <div class="workspace-stat__label">已公示批次</div>
        <div class="workspace-stat__note">当前能查看到评分结果的批次数量。</div>
      </article>
      <article class="workspace-stat">
        <span class="workspace-stat__eyebrow">Best Rank</span>
        <strong class="workspace-stat__value">{{ bestRank }}</strong>
        <div class="workspace-stat__label">最佳排名</div>
        <div class="workspace-stat__note">若暂无排名，则保持为待公布状态。</div>
      </article>
      <article class="workspace-stat">
        <span class="workspace-stat__eyebrow">Top Score</span>
        <strong class="workspace-stat__value">{{ bestScore }}</strong>
        <div class="workspace-stat__label">最高均分</div>
        <div class="workspace-stat__note">按当前已公布记录中的最高平均分展示。</div>
      </article>
      <article class="workspace-stat">
        <span class="workspace-stat__eyebrow">Reviewers</span>
        <strong class="workspace-stat__value">{{ totalTeachers }}</strong>
        <div class="workspace-stat__label">累计评分教师数</div>
        <div class="workspace-stat__note">已参与当前可见记录评分的教师总量。</div>
      </article>
    </section>

    <section class="workspace-section reveal-up reveal-delay-2">
      <div class="workspace-toolbar">
        <div class="workspace-toolbar__body">
          <h3 class="workspace-toolbar__title">评分记录表</h3>
          <p class="workspace-toolbar__desc">从各维度得分到综合排名，保持与教师评审侧一致的阅读顺序。</p>
        </div>
      </div>

      <el-table :data="rankList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="batchName" label="评分批次" width="180" />
        <el-table-column prop="workTitle" label="作品名称" min-width="160" />
        <el-table-column label="平均分" width="90">
          <template #default="{ row }">
            <span class="score-highlight">{{ row.avgScore ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创新性(25)" width="100">
          <template #default="{ row }">{{ row.avgInnovation ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="难度(25)" width="100">
          <template #default="{ row }">{{ row.avgDifficulty ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="完成度(30)" width="100">
          <template #default="{ row }">{{ row.avgCompletion ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="实用性(20)" width="100">
          <template #default="{ row }">{{ row.avgPracticality ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="排名" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.rankNo" :type="row.rankNo <= 3 ? 'danger' : 'info'" size="small">
              {{ row.rankNo }}
            </el-tag>
            <span v-else class="workspace-muted">待公布</span>
          </template>
        </el-table-column>
        <el-table-column label="评分教师数" width="100">
          <template #default="{ row }">{{ row.teacherCount ?? 0 }}</template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && !rankList.length" class="workspace-empty">
        <el-empty>
          <template #description>
            <p>暂无评分数据</p>
            <p style="font-size: 13px; color: var(--text-muted); margin-top: 6px">
              教师评分后即可查看结果，暂无数据请联系教师。
            </p>
          </template>
        </el-empty>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import request from '@/api/request'

interface MyRankItem {
  batchId: number
  batchName: string
  workId: number
  workTitle: string
  avgScore: number | null
  avgInnovation: number | null
  avgDifficulty: number | null
  avgCompletion: number | null
  avgPracticality: number | null
  teacherCount: number
  rankNo: number | null
}

const loading = ref(false)
const rankList = ref<MyRankItem[]>([])

const bestRank = computed(() => {
  const ranks = rankList.value.map((item) => item.rankNo).filter((value): value is number => typeof value === 'number')
  return ranks.length ? Math.min(...ranks) : '--'
})

const bestScore = computed(() => {
  const scores = rankList.value
    .map((item) => item.avgScore)
    .filter((value): value is number => typeof value === 'number')
  return scores.length ? Math.max(...scores).toFixed(1) : '--'
})

const totalTeachers = computed(() => rankList.value.reduce((sum, item) => sum + (item.teacherCount || 0), 0))

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/student/score/my-ranks')
    rankList.value = res.data || []
  } catch {
    rankList.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.my-ranking-page {
  max-width: 1120px;
  margin: 0 auto;
}

.score-highlight {
  font-weight: 600;
  color: var(--brand);
}
</style>
