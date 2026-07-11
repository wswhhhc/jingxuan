<template>
  <div class="workspace-page history-page">
    <section class="workspace-section workspace-section--soft reveal-up">
      <div class="workspace-toolbar">
        <div class="workspace-toolbar__body">
          <h2 class="workspace-toolbar__title">我的评分记录</h2>
          <p class="workspace-toolbar__desc">查看本人对作品的历史评分，并可快速回到评分工作区继续调整。</p>
        </div>
        <div class="workspace-toolbar__actions">
          <el-button type="primary" plain :loading="exporting" @click="handleExport">导出 CSV</el-button>
        </div>
      </div>
    </section>

    <section class="workspace-section reveal-up reveal-delay-1">
      <div class="workspace-toolbar workspace-toolbar--tight">
        <div class="workspace-toolbar__body">
          <h3 class="workspace-toolbar__title">评分记录表</h3>
          <p class="workspace-toolbar__desc">保留总分、维度分与评语，形成可回看的评审档案。</p>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="#" width="72">
          <template #default="{ $index }">
            {{ (query.page - 1) * query.size + $index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="workTitle" label="作品名" min-width="220" show-overflow-tooltip />
        <el-table-column prop="innovation" label="创新性" width="90" />
        <el-table-column prop="difficulty" label="技术难度" width="110" />
        <el-table-column prop="completion" label="完成度" width="90" />
        <el-table-column prop="practicality" label="实用性" width="90" />
        <el-table-column prop="total" label="总分" width="90">
          <template #default="{ row }">
            <span class="total-score">{{ row.total }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="评语" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.comment || '暂无评语' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="scoreTime" label="评分时间" width="180" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goScore(row)">查看并编辑评分</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && list.length === 0" class="workspace-empty">
        <el-empty description="暂无评分记录" :image-size="72" />
      </div>

      <PaginationBar v-model:page="query.page" v-model:size="query.size" :total="total" @change="reload" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getScoreHistory, type ScoreRecord } from '@/api/teacher/score'
import { useApiList } from '@/composables/useApiList'
import PaginationBar from '@/components/PaginationBar.vue'

const router = useRouter()
const exporting = ref(false)

const query = reactive({ page: 1, size: 10 })
const { loading, list, total, loadList } = useApiList<ScoreRecord, Parameters<typeof getScoreHistory>[0]>(
  getScoreHistory,
)
const reload = () => loadList({ page: query.page, size: query.size })

const goScore = (row: ScoreRecord) => {
  router.push({
    path: '/teacher/score',
    query: {
      workId: String(row.workId),
      batchId: row.batchId ? String(row.batchId) : undefined,
      from: 'history',
    },
  })
}

const escapeCsvCell = (value: unknown) => {
  const text = value == null ? '' : String(value)
  return `"${text.replace(/"/g, '""')}"`
}

const downloadCsv = (records: ScoreRecord[]) => {
  const headers = ['作品 ID', '作品名', '创新性', '技术难度', '完成度', '实用性', '总分', '评语', '评分时间']
  const rows = records.map((item) => [
    item.workId,
    item.workTitle,
    item.innovation,
    item.difficulty,
    item.completion,
    item.practicality,
    item.total,
    item.comment || '',
    item.scoreTime,
  ])
  const csv = [headers, ...rows].map((row) => row.map(escapeCsvCell).join(',')).join('\r\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  const now = new Date()
  link.href = url
  link.download = `teacher-score-history-${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

const handleExport = async () => {
  exporting.value = true
  try {
    const pageSize = 200
    const firstRes = await getScoreHistory({ page: 1, size: pageSize })
    const exportTotal = firstRes.data?.total || 0
    if (!exportTotal) {
      ElMessage.warning('暂无可导出的评分记录')
      return
    }

    let allRecords = [...(firstRes.data?.records || [])]
    const totalPages = Math.ceil(exportTotal / pageSize)
    for (let page = 2; page <= totalPages; page++) {
      const res = await getScoreHistory({ page, size: pageSize })
      allRecords = allRecords.concat(res.data?.records || [])
    }
    downloadCsv(allRecords)
    ElMessage.success(`已导出 ${allRecords.length} 条评分记录`)
  } finally {
    exporting.value = false
  }
}

onMounted(reload)
</script>

<style scoped>
.history-page {
  max-width: 1220px;
  margin: 0 auto;
}

.total-score {
  font-size: 16px;
  font-weight: 600;
  color: var(--brand);
}
</style>
