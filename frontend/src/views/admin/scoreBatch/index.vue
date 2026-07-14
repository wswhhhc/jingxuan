<template>
  <div class="score-batch-page">
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>评分批次列表</span>
          <el-button type="primary" @click="openWizard()">新建批次</el-button>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="batchName" label="批次名称" min-width="160" />
        <el-table-column label="开始时间" width="170">
          <template #default="{ row }">{{ row.startTime?.replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column label="结束时间" width="170">
          <template #default="{ row }">{{ row.endTime?.replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column prop="classScopes" label="适用范围" width="180" show-overflow-tooltip />
        <el-table-column label="待办" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.noticeTitle" type="success" size="small">已配置</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="400" fixed="right">
          <template #default="{ row }">
            <div class="batch-actions">
              <el-button size="small" @click="openWizard(row)">编辑</el-button>
              <el-button size="small" plain @click="viewDetail(row)">详情</el-button>
              <el-button size="small" type="primary" @click="viewScores(row)">评分明细</el-button>
              <el-button size="small" @click="editNotice(row)">待办</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <PaginationBar v-model:page="pageNum" v-model:size="pageSize" :total="total" @change="reload" />
      </div>
    </el-card>

    <!-- ===== 3 步向导弹窗 ===== -->
    <el-dialog
      v-model="wizardVisible"
      :title="wizardTitle"
      width="680px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-steps :active="step" finish-status="success" align-center style="margin-bottom: 28px">
        <el-step title="基本信息" />
        <el-step title="奖项配置" />
        <el-step title="待办要求" />
      </el-steps>

      <!-- Step 1: 基本信息 -->
      <el-form v-show="step === 0" :model="form" label-width="100px" ref="formRef" :rules="rules">
        <el-form-item label="批次名称" prop="batchName">
          <el-input v-model="form.batchName" placeholder="请输入批次名称" />
        </el-form-item>
        <el-form-item label="时间范围" prop="timeRange">
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="适用范围" prop="classScopes">
          <div class="scope-field">
            <el-switch
              v-model="form.allSchool"
              :active-value="true"
              :inactive-value="false"
              active-text="全校可参与"
              inactive-text="按班级参与"
            />
            <el-select
              v-model="form.classScopeIds"
              multiple
              filterable
              clearable
              collapse-tags
              collapse-tags-tooltip
              :disabled="form.allSchool"
              placeholder="请选择可参与班级"
              style="width: 100%"
            >
              <el-option v-for="item in classes" :key="item.id" :label="item.dictLabel" :value="item.dictValue" />
            </el-select>
            <div class="scope-field__hint">
              {{ form.allSchool ? '当前批次面向全校开放参与。' : '可多选班级，保存时会自动整理适用范围。' }}
            </div>
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
      </el-form>

      <!-- Step 2: 奖项配置 -->
      <div v-show="step === 1" class="step-prizes">
        <div class="step-prizes__toolbar">
          <span class="step-prizes__tip">配置该批次的奖项等级与奖品名额（可跳过，后续在"奖品配置"中管理）</span>
          <el-button size="small" type="primary" @click="addPrizeRow">新增奖项</el-button>
        </div>
        <el-table :data="prizeRows" stripe style="width: 100%">
          <el-table-column label="等级" width="100">
            <template #default="{ row }">
              <el-select v-model="row.rewardLevel" placeholder="等级" size="small" style="width: 90px">
                <el-option label="一等奖" value="一等奖" />
                <el-option label="二等奖" value="二等奖" />
                <el-option label="三等奖" value="三等奖" />
                <el-option label="优秀奖" value="优秀奖" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="奖项名称" width="140">
            <template #default="{ row }">
              <el-input v-model="row.rewardName" placeholder="如：第一名" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="奖品说明" min-width="200">
            <template #default="{ row }">
              <el-input v-model="row.prizeName" placeholder="如：荣誉证书 + 500元京东卡" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="名额" width="80">
            <template #default="{ row }">
              <el-input-number v-model="row.quota" :min="1" :max="999" size="small" style="width: 70px" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="60">
            <template #default="scope">
              <el-button size="small" type="danger" link @click="prizeRows.splice(scope.$index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="prizeRows.length === 0" class="step-prizes__empty">暂未配置奖项，可跳过此步骤</div>
      </div>

      <!-- Step 3: 待办要求 -->
      <div v-show="step === 2" class="step-notice">
        <div class="step-notice__tip">
          发布后将为该批次班级范围内的每位学生生成一条待办任务， 学生在待办列表中可查看到要求说明并提交作品。
          待办范围沿用第一步中设置的适用范围。
        </div>
        <el-form label-width="100px">
          <el-form-item label="待办标题">
            <el-input v-model="noticeTitle" placeholder="如：2026春学期作品提交" />
          </el-form-item>
          <el-form-item label="待办要求">
            <el-input
              v-model="noticeContent"
              type="textarea"
              :rows="10"
              placeholder="请填写作品要求、需上传的材料等说明…
建议包含以下内容：
1. 作品主题与要求
2. 需上传的材料（源码压缩包、演示视频、封面图等）
3. 材料格式与大小限制
4. 提交截止时间提醒"
            />
          </el-form-item>
        </el-form>
        <div class="step-notice__actions">
          <el-checkbox v-model="publishAfterSave">保存后立即发布待办</el-checkbox>
        </div>
      </div>

      <template #footer>
        <el-button v-if="step > 0" @click="step--">上一步</el-button>
        <el-button v-if="step < 2" type="primary" @click="nextStep">下一步</el-button>
        <el-button v-if="step === 2" type="primary" :loading="submitting" @click="handleWizardSubmit">
          完成创建
        </el-button>
        <el-button @click="wizardVisible = false">取消</el-button>
      </template>
    </el-dialog>

    <!-- 待办编辑快捷弹窗（从列表行操作进入） -->
    <el-dialog v-model="noticeVisible" title="编辑待办要求" width="600px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="待办标题">
          <el-input v-model="noticeTitle" placeholder="如：2026春学期作品提交" />
        </el-form-item>
        <el-form-item label="待办要求">
          <el-input
            v-model="noticeContent"
            type="textarea"
            :rows="10"
            placeholder="请填写作品要求、需上传的材料等说明…"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNoticeOnly">保存待办</el-button>
        <el-button type="success" @click="saveAndPublishNotice">保存并发布待办</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="批次详情" width="500px" destroy-on-close>
      <template v-if="detail">
        <div class="detail-grid">
          <div class="detail-item">
            <label>批次名称</label><span>{{ detail.batchName }}</span>
          </div>
          <div class="detail-item">
            <label>开始时间</label><span>{{ detail.startTime?.replace('T', ' ') }}</span>
          </div>
          <div class="detail-item">
            <label>结束时间</label><span>{{ detail.endTime?.replace('T', ' ') }}</span>
          </div>
          <div class="detail-item">
            <label>适用范围</label><span>{{ detail.classScopes || '-' }}</span>
          </div>
          <div class="detail-item">
            <label>待办标题</label><span>{{ detail.noticeTitle || '-' }}</span>
          </div>
          <div class="detail-item">
            <label>状态</label
            ><span>
              <el-tag :type="detail.status === 1 ? 'success' : 'info'" size="small">
                {{ detail.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </span>
          </div>
          <div class="detail-item">
            <label>创建时间</label><span>{{ detail.createTime?.replace('T', ' ') }}</span>
          </div>
        </div>
        <div v-if="detail.noticeContent" class="detail-notice-content">
          <label>待办内容</label>
          <pre>{{ detail.noticeContent }}</pre>
        </div>
      </template>
    </el-dialog>

    <!-- 评分明细弹窗 -->
    <el-dialog v-model="scoreVisible" title="评分明细" width="900px" destroy-on-close top="5vh">
      <template v-if="scoreBatchId">
        <div style="margin-bottom: 12px">
          <el-button type="success" size="small" @click="publishRanking(scoreBatchId)" :disabled="rankingPublished">
            {{ rankingPublished ? '已公示' : '公示排行榜' }}
          </el-button>
          <el-button size="small" @click="unpublishRanking(scoreBatchId)" :disabled="!rankingPublished">
            取消公示
          </el-button>
        </div>
        <el-table :data="scoreWorks" v-loading="scoreLoading" stripe style="width: 100%">
          <el-table-column type="expand">
            <template #default="{ row }">
              <el-table :data="row.scores" stripe size="small" v-if="row.scores?.length">
                <el-table-column prop="teacherName" label="评分教师" width="100" />
                <el-table-column prop="innovation" label="创新性(25)" width="100" />
                <el-table-column prop="difficulty" label="难度(25)" width="100" />
                <el-table-column prop="completion" label="完成度(30)" width="100" />
                <el-table-column prop="practicality" label="实用性(20)" width="100" />
                <el-table-column prop="total" label="总分" width="80" />
                <el-table-column prop="comment" label="评语" min-width="160" show-overflow-tooltip />
              </el-table>
              <el-empty v-else description="暂无评分" />
            </template>
          </el-table-column>
          <el-table-column prop="workTitle" label="作品名称" min-width="160" />
          <el-table-column prop="submitterName" label="提交人" width="100" />
          <el-table-column label="评分人数" width="80">
            <template #default="{ row }">{{ row.scores?.length || 0 }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { getBatchList, createBatch, updateBatch, getBatchScoreDetail, deleteBatch } from '@/api/admin/scoreBatch'
import { createPrize } from '@/api/admin/prize'
import type { ScoreBatchItem, BatchScoreDetail } from '@/api/admin/scoreBatch'
import request from '@/api/request'
import { useApiList } from '@/composables/useApiList'
import PaginationBar from '@/components/PaginationBar.vue'

const pageNum = ref(1)
const pageSize = ref(20)
const { loading, list, total, loadList } = useApiList<ScoreBatchItem, { page?: number; size?: number }>((params) =>
  getBatchList(params.page ?? pageNum.value, params.size ?? pageSize.value),
)
const reload = () => loadList({ page: pageNum.value, size: pageSize.value })

// ===== 向导 =====
const wizardVisible = ref(false)
const step = ref(0)
const isEdit = ref(false)
const editingId = ref<number | undefined>(undefined)
const submitting = ref(false)

const wizardTitle = computed(() => (isEdit.value ? '编辑批次' : '新建批次'))

const form = reactive({
  batchName: '',
  classScopes: '',
  allSchool: false,
  classScopeIds: [] as string[],
  status: 1 as number,
  id: undefined as number | undefined,
  timeRange: null as [string, string] | null,
})

const rules = {
  batchName: [{ required: true, message: '请输入批次名称', trigger: 'blur' }],
  timeRange: [{ required: true, message: '请选择时间范围', trigger: 'change' }],
  classScopes: [
    {
      validator: (_rule: unknown, _value: unknown, callback: (error?: Error) => void) => {
        if (form.allSchool || form.classScopeIds.length > 0) {
          callback()
          return
        }
        callback(new Error('请选择至少一个班级，或启用全校可参与'))
      },
      trigger: 'change',
    },
  ],
}

const formRef = ref<FormInstance>()
const classes = ref<{ id: number; dictLabel: string; dictValue: string }[]>([])

const ALL_SCHOOL_LABEL = '全校可参与'

// ===== 奖项 =====
interface PrizeRow {
  rewardLevel: string
  rewardName: string
  prizeName: string
  quota: number
}
const prizeRows = ref<PrizeRow[]>([])

function addPrizeRow() {
  prizeRows.value.push({ rewardLevel: '', rewardName: '', prizeName: '', quota: 1 })
}

// ===== 待办要求 =====
const noticeTitle = ref('')
const noticeContent = ref('')
const publishAfterSave = ref(true)

// 快捷待办编辑（从列表行进入）
const noticeVisible = ref(false)
const noticeBatchId = ref<number>(0)

// ===== 详情 =====
const detailVisible = ref(false)
const detail = ref<ScoreBatchItem | null>(null)

// ===== 评分明细 =====
const scoreVisible = ref(false)
const scoreLoading = ref(false)
const scoreBatchId = ref<number>(0)
const rankingPublished = ref(false)
const scoreWorks = ref<BatchScoreDetail[]>([])

// ===== 方法 =====

const normalizeClassScopes = (value?: string) => {
  const raw = (value || '').trim()
  if (!raw) return { allSchool: false, classScopeIds: [] as string[] }
  if (raw === ALL_SCHOOL_LABEL || raw === '全校' || raw.toLowerCase() === 'all') {
    return { allSchool: true, classScopeIds: [] as string[] }
  }
  return {
    allSchool: false,
    classScopeIds: raw
      .split(/[,，]/)
      .map((s) => s.trim())
      .filter(Boolean),
  }
}

const serializeClassScopes = () => {
  form.classScopes = form.allSchool ? ALL_SCHOOL_LABEL : form.classScopeIds.join(', ')
}

const loadClasses = async () => {
  try {
    const res = await request.get('/api/admin/dict/classes')
    classes.value = res.data || []
  } catch {
    classes.value = []
  }
}

// ===== 向导步骤 =====
async function nextStep() {
  if (step.value === 0) {
    const valid = await formRef.value?.validate().catch(() => false)
    if (!valid) return
  }
  step.value++
}

function openWizard(row?: ScoreBatchItem) {
  step.value = 0
  prizeRows.value = []
  noticeTitle.value = ''
  noticeContent.value = ''
  publishAfterSave.value = true
  submitting.value = false

  if (row) {
    isEdit.value = true
    editingId.value = row.id
    form.id = row.id
    form.batchName = row.batchName
    form.classScopes = row.classScopes
    const parsed = normalizeClassScopes(row.classScopes)
    form.allSchool = parsed.allSchool
    form.classScopeIds = parsed.classScopeIds
    form.status = row.status
    form.timeRange = [row.startTime, row.endTime]
    // 回填待办要求
    if (row.noticeTitle) noticeTitle.value = row.noticeTitle
    if (row.noticeContent) noticeContent.value = row.noticeContent
  } else {
    isEdit.value = false
    editingId.value = undefined
    form.id = undefined
    form.batchName = ''
    form.classScopes = ''
    form.allSchool = false
    form.classScopeIds = []
    form.status = 1
    form.timeRange = null
  }
  wizardVisible.value = true
}

async function handleWizardSubmit() {
  submitting.value = true
  try {
    serializeClassScopes()
    const { timeRange, ...rest } = form
    const data = {
      ...rest,
      classScopeIds: undefined,
      allSchool: undefined,
      startTime: timeRange?.[0],
      endTime: timeRange?.[1],
      noticeTitle: noticeTitle.value || undefined,
      noticeContent: noticeContent.value || undefined,
    }

    let batchId: number
    if (isEdit.value && form.id) {
      await updateBatch(data)
      batchId = form.id
      ElMessage.success('更新成功')
    } else {
      const res = await createBatch(data)
      batchId = res.data
      ElMessage.success('创建成功')
    }

    // 保存奖项
    for (const prize of prizeRows.value) {
      if (prize.rewardLevel && prize.rewardName) {
        try {
          await createPrize({ ...prize, batchId })
        } catch {
          /* skip individual failures */
        }
      }
    }

    // 保存并可选发布待办
    if (noticeTitle.value && noticeContent.value) {
      await request.put(`/score-batch/${batchId}/notice`, {
        noticeTitle: noticeTitle.value,
        noticeContent: noticeContent.value,
      })
      if (publishAfterSave.value) {
        try {
          await request.post(`/score-batch/${batchId}/publish-task`)
        } catch {
          /* may fail if no students yet */
        }
      }
    }

    wizardVisible.value = false
    reload()
  } catch {
    /* handled by interceptor */
  } finally {
    submitting.value = false
  }
}

// ===== 待办快捷编辑 =====
function editNotice(row: ScoreBatchItem) {
  noticeBatchId.value = row.id
  noticeTitle.value = row.noticeTitle || ''
  noticeContent.value = row.noticeContent || ''
  noticeVisible.value = true
}

async function saveNoticeOnly() {
  try {
    await request.put(`/score-batch/${noticeBatchId.value}/notice`, {
      noticeTitle: noticeTitle.value,
      noticeContent: noticeContent.value,
    })
    ElMessage.success('待办已保存')
    noticeVisible.value = false
    reload()
  } catch {
    /* handled */
  }
}

async function saveAndPublishNotice() {
  try {
    await request.put(`/score-batch/${noticeBatchId.value}/notice`, {
      noticeTitle: noticeTitle.value,
      noticeContent: noticeContent.value,
    })
    await request.post(`/score-batch/${noticeBatchId.value}/publish-task`)
    ElMessage.success('待办已保存并发布')
    noticeVisible.value = false
    reload()
  } catch {
    /* handled */
  }
}

// ===== 其他 =====
const viewDetail = (row: ScoreBatchItem) => {
  detail.value = row
  detailVisible.value = true
}

const handleDelete = async (row: ScoreBatchItem) => {
  try {
    await ElMessageBox.confirm(`确认删除批次「${row.batchName}」？`, '提示')
    await deleteBatch(row.id)
    ElMessage.success('删除成功')
    reload()
  } catch {
    /* cancelled or failed */
  }
}

const viewScores = async (row: ScoreBatchItem) => {
  scoreBatchId.value = row.id
  rankingPublished.value = row.rankPublished === 1
  scoreVisible.value = true
  scoreLoading.value = true
  try {
    const res = await getBatchScoreDetail(row.id)
    scoreWorks.value = res.data || []
  } catch {
    scoreWorks.value = []
  } finally {
    scoreLoading.value = false
  }
}

const publishRanking = async (batchId: number) => {
  try {
    await request.post(`/score-batch/${batchId}/publish-ranking`)
    ElMessage.success('排行榜已公示')
    rankingPublished.value = true
    reload()
  } catch {
    /* handled */
  }
}

const unpublishRanking = async (batchId: number) => {
  try {
    await request.post(`/score-batch/${batchId}/unpublish-ranking`)
    ElMessage.success('已取消公示')
    rankingPublished.value = false
    reload()
  } catch {
    /* handled */
  }
}

onMounted(() => {
  reload()
  loadClasses()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.text-muted {
  color: var(--text-muted);
}

.batch-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
  gap: 8px;
}
.batch-actions :deep(.el-button) {
  min-width: 0;
  margin-left: 0;
  padding-inline: 14px;
}

.scope-field {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 12px;
}
.scope-field__hint {
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.6;
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.detail-item label {
  display: block;
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}
.detail-item span {
  font-size: 14px;
  color: #333;
}
.detail-notice-content {
  margin-top: 16px;
}
.detail-notice-content label {
  display: block;
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}
.detail-notice-content pre {
  margin: 0;
  padding: 12px;
  border-radius: 8px;
  background: #f5f7fa;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.step-prizes__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.step-prizes__tip {
  font-size: 13px;
  color: var(--text-muted);
}
.step-prizes__empty {
  text-align: center;
  padding: 32px 0;
  color: var(--text-muted);
}

.step-notice__tip {
  font-size: 13px;
  color: var(--text-muted);
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 18px;
}
.step-notice__actions {
  margin-top: 12px;
}
</style>
