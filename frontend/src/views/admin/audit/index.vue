<template>
  <div class="audit-page">
    <section class="editorial-note reveal-up">
      审核管理页同时承担“通过 / 驳回 / 发布 / 精选”四种动作，因此把筛选区、列表区和详情弹窗统一到同一套档案式界面中。
    </section>

    <section class="filter-panel reveal-up reveal-delay-1">
      <div class="section-heading">
        <div>
          <h2 class="section-heading__title">作品筛选</h2>
          <p class="section-heading__meta">按状态、关键词、技术栈、班级与提交时间快速定位待审作品。</p>
        </div>
      </div>

      <el-form :model="query" inline class="audit-filters">
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 130px">
            <el-option label="待审核" :value="1" />
            <el-option label="已通过" :value="3" />
            <el-option label="已驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="作品名称/提交人" clearable />
        </el-form-item>
        <el-form-item label="技术栈">
          <el-input v-model="query.techStack" placeholder="技术栈" clearable />
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="query.classId" placeholder="全部" clearable style="width: 180px">
            <el-option v-for="c in classes" :key="c.id" :label="c.dictLabel" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="提交时间" class="audit-filters__range">
          <el-date-picker
            v-model="submitTimeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="起始"
            end-placeholder="截止"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item class="audit-filters__actions">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-panel reveal-up reveal-delay-2">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="审核列表" name="audit">
          <div class="section-heading">
            <div>
              <h2 class="section-heading__title">审核列表</h2>
              <p class="section-heading__meta">在列表中完成状态判断，在弹窗中查看完整信息与历史记录。</p>
            </div>
          </div>

          <el-table :data="list" v-loading="loading">
            <el-table-column label="编号" width="78">
              <template #default="{ $index }">
                <span>{{ getRowIndex($index) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="作品名称" min-width="180" show-overflow-tooltip />
            <el-table-column label="所属批次" width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <span>{{ row.batchName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="submitterName" label="提交人" width="110" />
            <el-table-column prop="techStack" label="技术栈" width="150" show-overflow-tooltip />
            <el-table-column label="提交时间" width="184">
              <template #default="{ row }">
                <span>{{ formatDateTime(row.submitTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="审核状态" width="108">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small">
                  {{ statusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="发布状态" width="108">
              <template #default="{ row }">
                <el-tag v-if="row.publishStatus === 1" type="success" size="small">已发布</el-tag>
                <el-tag v-else-if="row.publishStatus === 2" type="info" size="small">已下线</el-tag>
                <span v-else class="text-muted">未发布</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="250" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="showDetail(row)">详情</el-button>
                <el-button
                  size="small"
                  type="success"
                  :disabled="row.status !== 1"
                  @click="handleAudit(row, 'approved')"
                >
                  通过
                </el-button>
                <el-button
                  size="small"
                  type="danger"
                  :disabled="row.status !== 1"
                  @click="handleAudit(row, 'rejected')"
                >
                  驳回
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <PaginationBar v-model:page="query.page" v-model:size="query.size" :total="total" @change="reload" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="删除申请" name="delete-requests">
          <div class="section-heading">
            <div>
              <h2 class="section-heading__title">删除申请</h2>
              <p class="section-heading__meta">学生申请删除已通过的作品，需管理员审批后执行删除。</p>
            </div>
          </div>

          <el-table :data="drList" v-loading="drLoading">
            <el-table-column label="编号" width="60">
              <template #default="{ $index }">{{ drPageStart + $index + 1 }}</template>
            </el-table-column>
            <el-table-column prop="workTitle" label="作品名称" min-width="180" show-overflow-tooltip />
            <el-table-column prop="studentName" label="申请人" width="110" />
            <el-table-column prop="reason" label="申请原因" min-width="220" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'warning' : row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 0 ? '待处理' : row.status === 1 ? '已同意' : '已拒绝' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="申请时间" width="170">
              <template #default="{ row }">{{ row.createTime?.replace('T', ' ') }}</template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 0">
                  <el-button size="small" type="success" @click="handleApproveDelete(row)">同意</el-button>
                  <el-button size="small" type="danger" @click="handleOpenRejectDelete(row)">拒绝</el-button>
                </template>
                <span v-else class="text-muted">{{ row.adminReply || '-' }}</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <PaginationBar
              v-model:page="drPage"
              v-model:size="drPageSize"
              :total="drTotal"
              @change="loadDeleteRequests"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="detailVisible" title="作品详情" width="760px" destroy-on-close>
      <template v-if="detail">
        <div class="detail-grid">
          <div class="detail-item">
            <label>作品名称</label><span>{{ detail.title }}</span>
          </div>
          <div class="detail-item">
            <label>提交人</label><span>{{ detail.submitterName }}</span>
          </div>
          <div class="detail-item">
            <label>技术栈</label><span>{{ detail.techStack }}</span>
          </div>
          <div class="detail-item">
            <label>指导老师</label><span>{{ detail.advisor }}</span>
          </div>
          <div class="detail-item">
            <label>提交时间</label><span>{{ formatDateTime(detail.submitTime) }}</span>
          </div>
          <div class="detail-item">
            <label>发布状态</label><span>{{ detail.publishStatusLabel || '未发布' }}</span>
          </div>
          <div class="detail-item full">
            <label>服务器地址</label><span>{{ detail.previewUrl || '未填写' }}</span>
          </div>
          <div class="detail-item full">
            <label>简介</label>
            <p>{{ detail.summary }}</p>
          </div>
        </div>
        <el-divider />
        <el-timeline v-if="auditHistory.length > 0">
          <el-timeline-item
            v-for="h in auditHistory"
            :key="h.id"
            :timestamp="h.auditTime"
            :type="h.result === 1 ? 'success' : 'danger'"
          >
            <p>{{ h.result === 1 ? '审核通过' : '审核驳回' }}</p>
            <p v-if="h.reason" class="text-muted">原因：{{ h.reason }}</p>
            <p class="text-muted">审核人：{{ h.auditorName }}</p>
          </el-timeline-item>
        </el-timeline>
      </template>
      <template #footer>
        <div v-if="detail && detail.status === 1" class="dialog-actions">
          <el-button type="success" @click="submitAudit(detail!.id, 'approved')">审核通过</el-button>
          <el-button type="danger" @click="rejectDialogVisible = true">驳回</el-button>
        </div>
        <div v-if="detail && detail.publishStatus === 0 && detail.status === 3" class="dialog-actions">
          <el-button type="primary" @click="handlePublish(detail!.id)">发布展示</el-button>
        </div>
        <div v-if="detail && detail.publishStatus === 1" class="dialog-actions">
          <el-button :type="detail.featured === 1 ? 'warning' : 'default'" @click="handleFeatured(detail)">
            {{ detail.featured === 1 ? '取消精选' : '设为精选' }}
          </el-button>
          <el-button @click="handleOffline(detail!.id)">下线</el-button>
        </div>
        <div
          v-if="detail"
          class="dialog-actions"
          style="margin-top: 8px; border-top: 1px solid var(--border-subtle, #e4e7ed); padding-top: 8px"
        >
          <el-button type="danger" size="small" plain @click="handleAdminDelete(detail!.id)">删除作品</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectDialogVisible" title="驳回原因" width="420px">
      <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请填写驳回原因，以便学生修改" />
      <template #footer>
        <div class="dialog-actions">
          <el-button @click="rejectDialogVisible = false">取消</el-button>
          <el-button type="danger" @click="submitAudit(rejectWorkId!, 'rejected')">确认驳回</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="featuredDialogVisible" title="设置精选作品" width="460px">
      <el-form label-width="100px">
        <el-form-item label="作品名称">
          <span>{{ featuredForm.title }}</span>
        </el-form-item>
        <el-form-item label="精选状态">
          <el-switch
            v-model="featuredForm.enabled"
            :active-value="1"
            :inactive-value="0"
            active-text="精选"
            inactive-text="普通"
          />
        </el-form-item>
        <el-form-item label="访问地址">
          <el-input v-model="featuredForm.previewUrl" placeholder="请输入服务器访问地址，如 http://ip:port" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-actions">
          <el-button @click="featuredDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleFeaturedSubmit">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 拒绝删除申请弹窗 -->
    <el-dialog v-model="drRejectVisible" title="拒绝删除申请" width="460px">
      <p style="margin-bottom: 12px; color: var(--text-secondary)">作品：{{ drCurrentItem?.workTitle }}</p>
      <el-input
        v-model="drRejectReply"
        type="textarea"
        :rows="4"
        placeholder="请填写拒绝原因"
        maxlength="500"
        show-word-limit
      />
      <template #footer>
        <el-button @click="drRejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="drSubmitting" @click="handleRejectDelete">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'
import {
  getAuditList,
  doAudit,
  getAuditDetail,
  getAuditHistory,
  publishWork,
  offlineWork,
  setFeatured,
} from '@/api/admin/audit'
import type { AuditQuery, AuditHistoryItem } from '@/api/admin/audit'
import type { WorkListVO, WorkDetailVO } from '@/api/types'
import { useApiList } from '@/composables/useApiList'
import PaginationBar from '@/components/PaginationBar.vue'
import { formatDateTime as formatDateTimeValue } from '@/utils/format'

const detail = ref<WorkDetailVO | null>(null)
const detailVisible = ref(false)
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const rejectWorkId = ref<number | null>(null)
const auditHistory = ref<AuditHistoryItem[]>([])

const query = reactive<AuditQuery>({
  page: 1,
  size: 20,
  status: undefined,
  keyword: '',
  techStack: '',
})

const classes = ref<{ id: number; dictLabel: string }[]>([])
const submitTimeRange = ref<[string, string] | null>(null)
const { loading, list, total, loadList } = useApiList<WorkListVO, AuditQuery>(getAuditList)
const reload = () => {
  const params: AuditQuery = { ...query }
  if (submitTimeRange.value) {
    params.submitTimeBegin = submitTimeRange.value[0]
    params.submitTimeEnd = submitTimeRange.value[1]
  }
  loadList(params)
}

const statusType = (s: number) => {
  const map: Record<number, string> = { 1: 'warning', 3: 'success', 2: 'danger', 0: 'info' }
  return map[s] || 'info'
}
const statusLabel = (s: number) => {
  const map: Record<number, string> = { 0: '草稿', 1: '待审核', 2: '已驳回', 3: '已通过' }
  return map[s] || '未知'
}

const getRowIndex = (index: number) => {
  const currentPage = query.page ?? 1
  const currentSize = query.size ?? 20
  return (currentPage - 1) * currentSize + index + 1
}

const formatDateTime = (value?: string) => formatDateTimeValue(value, '未提交')

async function loadClasses() {
  try {
    const res = await request.get('/api/admin/dict/classes')
    classes.value = res.data || []
  } catch {
    classes.value = []
  }
}

const handleSearch = () => {
  query.page = 1
  reload()
}

const handleReset = () => {
  query.page = 1
  query.status = undefined
  query.keyword = ''
  query.techStack = ''
  query.classId = undefined
  submitTimeRange.value = null
  reload()
}

const showDetail = async (row: WorkListVO) => {
  try {
    const res = await getAuditDetail(row.id)
    detail.value = res.data
    const historyRes = await getAuditHistory(row.id)
    auditHistory.value = historyRes.data?.records || []
  } catch (e) {
    console.error('加载审核详情失败:', e)
    detail.value = null
  }
  detailVisible.value = true
}

const handleAudit = (row: WorkListVO, result: 'approved' | 'rejected') => {
  if (result === 'rejected') {
    rejectReason.value = ''
    rejectWorkId.value = row.id
    rejectDialogVisible.value = true
  } else {
    ElMessageBox.confirm('确认通过该作品审核？', '提示').then(() => submitAudit(row.id, 'approved'))
  }
}

const submitAudit = async (workId: number, result: 'approved' | 'rejected') => {
  try {
    await doAudit({ workId, result, reason: result === 'rejected' ? rejectReason.value : undefined })
    ElMessage.success(result === 'approved' ? '审核通过' : '已驳回')
    rejectDialogVisible.value = false
    detailVisible.value = false
    reload()
  } catch (e) {
    console.error('审核操作失败:', e)
    ElMessage.error('审核操作失败，请重试')
  }
}

const handlePublish = async (workId: number) => {
  try {
    await publishWork(workId)
    ElMessage.success('发布成功')
    detailVisible.value = false
    reload()
  } catch (e) {
    console.error('发布失败:', e)
    ElMessage.error('发布失败，请重试')
  }
}

const handleOffline = async (workId: number) => {
  try {
    await offlineWork(workId)
    ElMessage.success('已下线')
    detailVisible.value = false
    reload()
  } catch (e) {
    console.error('下线失败:', e)
    ElMessage.error('下线失败，请重试')
  }
}

const featuredDialogVisible = ref(false)
const featuredForm = reactive({
  title: '',
  enabled: 0 as 0 | 1,
  previewUrl: '',
})

const handleFeatured = (work: WorkDetailVO) => {
  featuredForm.title = work.title
  featuredForm.enabled = work.featured === 1 ? 1 : 0
  featuredForm.previewUrl = work.previewUrl || ''
  featuredDialogVisible.value = true
}

const handleFeaturedSubmit = async () => {
  try {
    await setFeatured(detail.value!.id, featuredForm.enabled, featuredForm.previewUrl)
    ElMessage.success(featuredForm.enabled ? '已设为精选' : '已取消精选')
    featuredDialogVisible.value = false
    const res = await getAuditDetail(detail.value!.id)
    detail.value = res.data
  } catch (e) {
    console.error('设置精选失败:', e)
    ElMessage.error('操作失败，请重试')
  }
}

async function handleAdminDelete(workId: number) {
  try {
    await ElMessageBox.confirm(
      '确认删除该作品？此操作将同时删除关联的附件、评分、评论等所有数据，不可恢复！',
      '危险操作',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
      },
    )
    await request.delete(`/api/admin/work/${workId}`)
    ElMessage.success('作品已删除')
    detailVisible.value = false
    reload()
  } catch {
    /* cancelled or failed */
  }
}

// ===== 删除申请管理 =====
interface DeleteRequestItem {
  id: number
  workTitle: string
  studentName: string
  reason: string
  status: number
  adminReply?: string
}

const activeTab = ref('audit')
const drLoading = ref(false)
const drList = ref<DeleteRequestItem[]>([])
const drTotal = ref(0)
const drPage = ref(1)
const drPageSize = ref(10)
const drCurrentItem = ref<DeleteRequestItem | null>(null)
const drRejectVisible = ref(false)
const drRejectReply = ref('')
const drSubmitting = ref(false)

const drPageStart = computed(() => (drPage.value - 1) * drPageSize.value)

function onTabChange(name: string) {
  if (name === 'delete-requests') {
    loadDeleteRequests()
  }
}

async function loadDeleteRequests() {
  drLoading.value = true
  try {
    const res = await request.get<{ records: DeleteRequestItem[]; total: number }>('/api/admin/delete-requests', {
      params: { page: drPage.value, size: drPageSize.value, status: 0 },
    })
    drList.value = res.data?.records || []
    drTotal.value = res.data?.total || 0
  } catch {
    drList.value = []
  } finally {
    drLoading.value = false
  }
}

async function handleApproveDelete(row: DeleteRequestItem) {
  try {
    await ElMessageBox.confirm(`确认同意删除作品《${row.workTitle}》？此操作不可恢复！`, '确认同意', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
    })
    await request.post(`/api/admin/delete-request/${row.id}/approve`)
    ElMessage.success('已同意删除申请，作品已删除')
    loadDeleteRequests()
  } catch {
    /* cancelled or failed */
  }
}

function handleOpenRejectDelete(row: DeleteRequestItem) {
  drCurrentItem.value = row
  drRejectReply.value = ''
  drRejectVisible.value = true
}

async function handleRejectDelete() {
  if (!drCurrentItem.value) return
  if (!drRejectReply.value.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  drSubmitting.value = true
  try {
    await request.post(`/api/admin/delete-request/${drCurrentItem.value.id}/reject`, {
      reply: drRejectReply.value,
    })
    ElMessage.success('已拒绝删除申请')
    drRejectVisible.value = false
    loadDeleteRequests()
  } finally {
    drSubmitting.value = false
  }
}

onMounted(() => {
  loadClasses()
  reload()
})
</script>

<style scoped>
.audit-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.audit-filters :deep(.el-form-item) {
  margin-bottom: 0;
}

.audit-filters__range {
  min-width: 340px;
}

.audit-filters__actions {
  margin-top: 8px;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detail-item {
  padding: 14px 16px;
  border: 1px solid var(--border-subtle);
  border-radius: 16px;
  background: color-mix(in srgb, var(--card-bg) 82%, transparent);
}

.detail-item label {
  display: block;
  margin-bottom: 8px;
  color: var(--text-muted);
  font-size: 12px;
}

.detail-item.full {
  grid-column: 1 / -1;
}

.detail-item p {
  margin: 0;
  line-height: 1.85;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 768px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .audit-filters__range {
    min-width: 0;
  }
}
</style>
