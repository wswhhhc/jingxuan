<template>
  <div class="workspace-page ranking-page">
    <section class="workspace-section workspace-filter-panel reveal-up">
      <div class="workspace-toolbar">
        <div class="workspace-toolbar__body">
          <h2 class="workspace-toolbar__title">排行榜筛选</h2>
          <p class="workspace-toolbar__desc">按批次和分类切换榜单视角，必要时手动刷新当前排行结果。</p>
        </div>
      </div>

      <el-form :model="query" inline>
        <el-form-item label="批次">
          <el-select
            v-model="query.batchId"
            placeholder="选择批次"
            clearable
            style="width: 240px"
            @change="onBatchChange"
          >
            <el-option v-for="b in batches" :key="b.id" :label="b.batchName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 160px" @change="reload">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item class="workspace-filter-actions">
          <el-button type="primary" @click="reload">查询</el-button>
          <el-button @click="handleRefresh">刷新排行</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="workspace-section reveal-up reveal-delay-1">
      <div class="workspace-toolbar workspace-toolbar--tight">
        <div class="workspace-toolbar__body">
          <h3 class="workspace-toolbar__title">排行榜</h3>
          <p class="workspace-toolbar__desc">综合得分排名，取所有评分教师平均分，保留各维度得分线索。</p>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="排名" width="70">
          <template #default="{ row }">
            <span v-if="row.rankNo <= 3" class="rank-medal">{{ ['🥇', '🥈', '🥉'][row.rankNo - 1] }}</span>
            <span v-else class="rank-num">{{ row.rankNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="workTitle" label="作品名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="techStack" label="技术栈" width="130" show-overflow-tooltip />
        <el-table-column label="各维度得分" min-width="220">
          <template #default="{ row }">
            <div class="dim-scores">
              <span title="创新性">创{{ row.avgInnovation }}</span>
              <span title="技术难度">技{{ row.avgDifficulty }}</span>
              <span title="完成度">完{{ row.avgCompletion }}</span>
              <span title="实用性">实{{ row.avgPracticality }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="综合得分" width="100">
          <template #default="{ row }">
            <span class="total-score">{{ row.avgScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="获奖等级" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.rewardLevel" :type="rewardTagType(row.rewardLevel)" size="small">{{
              row.rewardLevel
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="teacherCount" label="评分教师数" width="90" />
      </el-table>

      <div v-if="!query.batchId && !loading" class="workspace-empty">
        <el-empty description="请先选择批次查看排行" />
      </div>
      <div v-else-if="list.length === 0 && !loading" class="workspace-empty">
        <el-empty description="暂无排行数据，请先确认已有评分结果" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRanking, getRankingBatches, getRankingCategories, refreshRanking } from '@/api/teacher/ranking'
import type { RankItem, CategoryItem } from '@/api/types'
import { useApiList } from '@/composables/useApiList'

import { rewardTagType } from '@/utils/format'
const batches = ref<{ id: number; batchName: string }[]>([])
const categories = ref<CategoryItem[]>([])

const query = reactive({
  batchId: undefined as number | undefined,
  type: undefined as string | undefined,
})
const { loading, list, loadList } = useApiList<RankItem, Parameters<typeof getRanking>[0], RankItem[]>(
  getRanking,
  (data) => ({
    records: data || [],
    total: data?.length || 0,
  }),
)
const reload = () => loadList({ ...query, topN: 50 })

const loadBatches = async () => {
  try {
    const res = await getRankingBatches()
    batches.value = res.data || []
  } catch (e) {
    console.error('加载排名数据失败:', e)
  }
}

const loadCategories = async () => {
  try {
    const res = await getRankingCategories(query.batchId)
    categories.value = res.data || []
  } catch (e) {
    console.error('加载排名数据失败:', e)
  }
}

// 批次切换时重新加载分类
const onBatchChange = (val: number | undefined) => {
  query.batchId = val
  loadCategories()
  reload()
}

const handleRefresh = async () => {
  if (!query.batchId) {
    ElMessage.warning('请先选择批次')
    return
  }
  try {
    await refreshRanking(query.batchId)
    ElMessage.success('排行已刷新')
    reload()
  } catch (e) {
    console.error('刷新排行失败:', e)
    ElMessage.error('刷新失败，请重试')
  }
}

onMounted(() => {
  loadBatches()
  loadCategories()
})
</script>

<style scoped>
.ranking-page {
  max-width: 1220px;
  margin: 0 auto;
}

.rank-medal {
  font-size: 22px;
}

.rank-num {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.total-score {
  font-size: 18px;
  font-weight: 700;
  color: var(--brand);
}

.dim-scores {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.dim-scores span {
  padding: 4px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--brand-soft) 70%, transparent);
}
</style>
