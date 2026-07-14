<template>
  <div class="workspace-page prize-page">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="奖项配置" name="config">
        <section class="workspace-section reveal-up">
          <div class="workspace-toolbar workspace-toolbar--tight">
            <div class="workspace-toolbar__body">
              <h3 class="workspace-toolbar__title">奖项配置</h3>
              <p class="workspace-toolbar__desc">按批次整理奖项、奖品与名额配置。</p>
            </div>
            <div class="workspace-toolbar__actions">
              <el-select v-model="batchFilter" clearable placeholder="筛选批次" @change="reload">
                <el-option v-for="b in batches" :key="b.id" :label="b.batchName" :value="b.id" />
              </el-select>
              <el-button type="primary" @click="showEdit()">新增奖项</el-button>
            </div>
          </div>

          <el-table :data="list" v-loading="loading" stripe>
            <el-table-column prop="batchName" label="批次" width="140" />
            <el-table-column prop="rewardLevel" label="等级" width="80">
              <template #default="{ row }">
                <el-tag :type="rewardTagType(row.rewardLevel)" size="small">{{ row.rewardLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="rewardName" label="奖项名称" width="120" />
            <el-table-column prop="prizeName" label="奖品" min-width="200" show-overflow-tooltip />
            <el-table-column prop="quota" label="名额" width="70" />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="showEdit(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="workspace-pagination">
            <PaginationBar v-model:page="page" v-model:size="size" :total="total" @change="reload" />
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="发放追踪" name="issue">
        <section class="workspace-section reveal-up">
          <div class="workspace-toolbar workspace-toolbar--tight">
            <div class="workspace-toolbar__body">
              <h3 class="workspace-toolbar__title">发放追踪</h3>
              <p class="workspace-toolbar__desc">跟踪奖项发放与取消状态，保留领奖记录。</p>
            </div>
            <div class="workspace-toolbar__actions">
              <el-select v-model="issueRewardFilter" clearable placeholder="筛选奖品" @change="loadIssueList">
                <el-option v-for="b in batches" :key="b.id" :label="b.batchName" :value="b.id" />
              </el-select>
              <el-button type="primary" @click="showIssueDialog()">发放奖品</el-button>
            </div>
          </div>

          <el-table :data="issueList" v-loading="issueLoading" stripe>
            <el-table-column label="编号" width="70">
              <template #default="{ $index }">
                {{ getIssueRowIndex($index) }}
              </template>
            </el-table-column>
            <el-table-column prop="rewardName" label="奖项" width="120" />
            <el-table-column prop="workTitle" label="作品" min-width="160" show-overflow-tooltip />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.issueStatus === 1 ? 'success' : 'info'" size="small">
                  {{ row.issueStatus === 1 ? '已发放' : '已取消' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="issueTime" label="发放时间" width="170" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.issueStatus === 1" size="small" type="warning" @click="handleCancelIssue(row)"
                  >取消发放</el-button
                >
                <span v-else style="color: #c0c4cc">--</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="workspace-pagination">
            <PaginationBar
              v-model:page="issuePage"
              v-model:size="issueSize"
              :total="issueTotal"
              @change="reloadIssue"
            />
          </div>
        </section>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="editVisible" :title="isEdit ? '编辑奖项' : '新增奖项'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="批次">
          <el-select v-model="form.batchId" placeholder="选择批次" style="width: 100%">
            <el-option v-for="b in batches" :key="b.id" :label="b.batchName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="等级">
          <el-select v-model="form.rewardLevel" placeholder="选择等级" style="width: 100%">
            <el-option label="一等奖" value="一等奖" />
            <el-option label="二等奖" value="二等奖" />
            <el-option label="三等奖" value="三等奖" />
            <el-option label="优秀奖" value="优秀奖" />
          </el-select>
        </el-form-item>
        <el-form-item label="奖项名称">
          <el-input v-model="form.rewardName" placeholder="如：最佳创新奖" />
        </el-form-item>
        <el-form-item label="奖品">
          <el-input v-model="form.prizeName" placeholder="奖品名称" />
        </el-form-item>
        <el-form-item label="名额">
          <el-input-number v-model="form.quota" :min="1" :max="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="issueVisible" title="发放奖品" width="640px">
      <el-form :model="issueForm" label-width="80px">
        <el-form-item label="奖品">
          <el-select v-model="issueForm.rewardId" placeholder="选择奖品" style="width: 100%" @change="onRewardChange">
            <el-option v-for="p in list" :key="p.id" :label="`${p.batchName} - ${p.rewardName}`" :value="p.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <div v-if="rankedWorks.length > 0" class="ranked-works">
        <h4 style="margin: 0 0 8px">选择获奖作品（按分数排名）</h4>
        <el-table
          :data="rankedWorks"
          v-loading="rankedLoading"
          stripe
          highlight-current-row
          @row-click="selectRankedWork"
          max-height="320"
        >
          <el-table-column label="排名" width="60">
            <template #default="{ row }">
              <span v-if="row.rankNo <= 3" style="font-size: 18px">{{ ['🥇', '🥈', '🥉'][row.rankNo - 1] }}</span>
              <span v-else>{{ row.rankNo }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="workTitle" label="作品名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="techStack" label="技术栈" width="110" show-overflow-tooltip />
          <el-table-column label="平均分" width="80">
            <template #default="{ row }">
              <span style="font-weight: 600; color: var(--brand)">{{ row.avgScore }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-else-if="issueForm.rewardId && !rankedLoading" description="暂无排名数据，请先确认该批次已有评分" />

      <template #footer>
        <el-button @click="issueVisible = false">取消</el-button>
        <el-button type="primary" :loading="issueSaving" :disabled="!selectedRankedWork" @click="handleIssueSubmit"
          >确认发放</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/app/session/auth'
import {
  getPrizeList,
  createPrize,
  updatePrize,
  deletePrize,
  getPrizeBatches,
  getIssueList,
  issuePrize,
  cancelIssue,
  getRankedWorks,
} from '@/api/admin/prize'
import type { PrizeItem, IssueItem, RankedWork } from '@/api/admin/prize'
import { useApiList } from '@/composables/useApiList'
import { useCrudDialog } from '@/composables/useCrudDialog'
import PaginationBar from '@/components/PaginationBar.vue'

import { rewardTagType } from '@/utils/format'
const authStore = useAuthStore()

const activeTab = ref('config')
const batches = ref<{ id: number; batchName: string }[]>([])
const batchFilter = ref<number | ''>('')
const page = ref(1)
const size = ref(20)
const { loading, list, total, loadList } = useApiList<PrizeItem, Parameters<typeof getPrizeList>[0]>(getPrizeList)
const reload = () =>
  loadList({ page: page.value, size: size.value, ...(batchFilter.value ? { batchId: batchFilter.value } : {}) })
const { editVisible, isEdit, editId, openCreate, openEdit, save } = useCrudDialog()
const form = reactive({
  batchId: 0,
  rewardLevel: '',
  rewardName: '',
  prizeName: '',
  quota: 1,
})

// issue tracking
const issueLoading = ref(false)
const issueList = ref<IssueItem[]>([])
const issueTotal = ref(0)
const issuePage = ref(1)
const issueSize = ref(20)
const issueRewardFilter = ref<number | ''>('')
const issueVisible = ref(false)
const issueSaving = ref(false)
const issueForm = reactive({ rewardId: 0 })

// 排名作品列表（发放奖品时选择）
const rankedWorks = ref<RankedWork[]>([])
const rankedLoading = ref(false)
const selectedRankedWork = ref<RankedWork | null>(null)

const onRewardChange = async (rewardId: number) => {
  selectedRankedWork.value = null
  rankedWorks.value = []
  const prize = list.value.find((p) => p.id === rewardId)
  if (!prize?.batchId) return
  rankedLoading.value = true
  try {
    const res = await getRankedWorks({ batchId: prize.batchId, topN: 50 })
    rankedWorks.value = (res.data as RankedWork[]) || []
  } catch {
    rankedWorks.value = []
  } finally {
    rankedLoading.value = false
  }
}

const selectRankedWork = (row: RankedWork) => {
  selectedRankedWork.value = row
}

const reloadIssue = async () => {
  issueLoading.value = true
  try {
    const res = await getIssueList({ page: issuePage.value, size: issueSize.value })
    issueList.value = (res.data?.records as IssueItem[]) || []
    issueTotal.value = res.data?.total || 0
  } catch (e) {
    console.error('加载发放记录失败:', e)
  } finally {
    issueLoading.value = false
  }
}

const loadBatches = async () => {
  try {
    const res = await getPrizeBatches()
    batches.value = res.data || []
  } catch (e) {
    console.error('加载批次列表失败:', e)
  }
}

const showEdit = (row?: PrizeItem) => {
  if (row) {
    openEdit(row.id)
    form.batchId = row.batchId
    form.rewardLevel = row.rewardLevel
    form.rewardName = row.rewardName
    form.prizeName = row.prizeName
    form.quota = row.quota
  } else {
    openCreate()
    form.batchId = 0
    form.rewardLevel = ''
    form.rewardName = ''
    form.prizeName = ''
    form.quota = 1
  }
}

const handleSave = async () => {
  if (!form.batchId || !form.rewardLevel) {
    ElMessage.warning('请填写完整')
    return
  }
  const ok = await save(() => (isEdit.value ? updatePrize(editId.value, form) : createPrize(form)), { label: '奖项' })
  if (ok) reload()
}

const handleDelete = async (row: PrizeItem) => {
  try {
    await ElMessageBox.confirm('确认删除此奖项配置？', '提示')
  } catch {
    return // 用户取消
  }
  try {
    await deletePrize(row.id)
    ElMessage.success('已删除')
    reload()
  } catch (e) {
    console.error('删除奖项失败:', e)
    ElMessage.error('删除失败，请重试')
  }
}

const loadIssueList = async () => {
  issueLoading.value = true
  try {
    const params: Parameters<typeof getIssueList>[0] = { page: issuePage.value, size: issueSize.value }
    if (issueRewardFilter.value) params.rewardId = issueRewardFilter.value
    const res = await getIssueList(params)
    issueList.value = res.data?.records || []
    issueTotal.value = res.data?.total || 0
  } finally {
    issueLoading.value = false
  }
}

const getIssueRowIndex = (index: number) => (issuePage.value - 1) * issueSize.value + index + 1

const showIssueDialog = () => {
  issueForm.rewardId = 0
  selectedRankedWork.value = null
  rankedWorks.value = []
  issueVisible.value = true
}

const handleIssueSubmit = async () => {
  if (!issueForm.rewardId || !selectedRankedWork.value) {
    ElMessage.warning('请选择奖品和作品')
    return
  }
  issueSaving.value = true
  try {
    await issuePrize({
      rewardId: issueForm.rewardId,
      workId: selectedRankedWork.value.workId,
      operatorId: Number(authStore.userInfo?.id) || 0,
    })
    ElMessage.success(`已向「${selectedRankedWork.value.workTitle}」发放奖品`)
    issueVisible.value = false
    selectedRankedWork.value = null
    loadIssueList()
  } catch (e) {
    console.error('发放奖品失败:', e)
    ElMessage.error('发放失败，请重试')
  } finally {
    issueSaving.value = false
  }
}

const handleCancelIssue = async (row: IssueItem) => {
  try {
    await ElMessageBox.confirm('确认取消该发放记录？', '提示')
  } catch {
    return // 用户取消
  }
  try {
    await cancelIssue(row.id)
    ElMessage.success('已取消')
    loadIssueList()
  } catch (e) {
    console.error('取消发放失败:', e)
    ElMessage.error('操作失败，请重试')
  }
}

const onTabChange = (tab: string) => {
  if (tab === 'issue') loadIssueList()
}

onMounted(() => {
  loadBatches()
  reload()
})
</script>

<style scoped>
.prize-page {
  max-width: 1240px;
  margin: 0 auto;
}
</style>
