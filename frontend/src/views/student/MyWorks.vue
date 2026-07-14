<template>
  <div class="workspace-page my-works">
    <section class="workspace-section workspace-section--soft reveal-up">
      <div class="workspace-intro">
        <div class="workspace-intro__body">
          <span class="workspace-intro__eyebrow">Creation Archive</span>
          <h2 class="workspace-intro__title">我的作品档案</h2>
          <p class="workspace-intro__summary">
            把草稿、审核中与已发布内容放回同一条创作时间线里，便于继续修改、提交和回看展示状态。
          </p>
        </div>
        <el-button type="primary" @click="handleCreateWork">
          <el-icon><Plus /></el-icon>提交新作品
        </el-button>
      </div>
    </section>

    <section class="workspace-stats reveal-up reveal-delay-1">
      <article class="workspace-stat">
        <span class="workspace-stat__eyebrow">Archive Total</span>
        <strong class="workspace-stat__value">{{ total }}</strong>
        <div class="workspace-stat__label">作品总数</div>
        <div class="workspace-stat__note">当前账号下已建立的作品档案。</div>
      </article>
      <article class="workspace-stat">
        <span class="workspace-stat__eyebrow">Awaiting Review</span>
        <strong class="workspace-stat__value">{{ statusStats.submitted }}</strong>
        <div class="workspace-stat__label">审核中</div>
        <div class="workspace-stat__note">已提交并等待管理员处理的作品。</div>
      </article>
      <article class="workspace-stat">
        <span class="workspace-stat__eyebrow">Needs Revision</span>
        <strong class="workspace-stat__value">{{ statusStats.rejected }}</strong>
        <div class="workspace-stat__label">待修改</div>
        <div class="workspace-stat__note">被驳回后可以继续补充或重提的作品。</div>
      </article>
      <article class="workspace-stat">
        <span class="workspace-stat__eyebrow">Published</span>
        <strong class="workspace-stat__value">{{ statusStats.approved }}</strong>
        <div class="workspace-stat__label">已通过</div>
        <div class="workspace-stat__note">已完成审核并进入展示节奏的作品。</div>
      </article>
    </section>

    <section class="workspace-section reveal-up reveal-delay-2">
      <div class="workspace-toolbar">
        <div class="workspace-toolbar__body">
          <h3 class="workspace-toolbar__title">状态筛选</h3>
          <p class="workspace-toolbar__desc">按创作阶段快速切换列表，让当前最需要处理的内容更靠前。</p>
        </div>
        <div class="workspace-toolbar__actions">
          <el-radio-group v-model="filterStatus" @change="handleFilterChange">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="draft">草稿</el-radio-button>
            <el-radio-button value="submitted">审核中</el-radio-button>
            <el-radio-button value="rejected">已驳回</el-radio-button>
            <el-radio-button value="approved">已通过</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <div v-loading="loading">
        <div v-if="list.length > 0" class="workspace-collection">
          <article v-for="item in list" :key="item.id" class="workspace-list-item">
            <div class="workspace-list-item__body">
              <h4 class="workspace-list-item__title">
                <router-link :to="getDetailRoute(item.id)">{{ item.title }}</router-link>
              </h4>
              <div class="workspace-list-item__meta">
                <span>{{ item.techStack || '未填写技术栈' }}</span>
                <span>指导教师：{{ item.advisor || '未填写' }}</span>
                <span>创建时间：{{ item.createTime }}</span>
              </div>
            </div>

            <div class="workspace-list-item__aside">
              <el-tag v-if="item.status === 'draft'" type="info" effect="plain">草稿</el-tag>
              <el-tag v-else-if="item.status === 'submitted'" type="warning" effect="plain">审核中</el-tag>
              <el-tag v-else-if="item.status === 'rejected'" type="danger" effect="plain">已驳回</el-tag>
              <el-tag v-else type="success" effect="plain">已通过</el-tag>

              <div class="workspace-list-item__actions">
                <el-button type="success" link @click="router.push(getDetailRoute(item.id))">查看</el-button>
                <el-button
                  v-if="canManage(item) && (item.status === 'draft' || item.status === 'rejected')"
                  type="primary"
                  link
                  @click="router.push(`/student/works/edit/${item.id}`)"
                >
                  编辑
                </el-button>
                <el-button v-if="canManage(item) && item.status === 'submitted'" type="info" link disabled>
                  审核中
                </el-button>
                <el-button
                  v-if="canManage(item) && item.status === 'approved'"
                  type="warning"
                  link
                  @click="openDeleteRequest(item)"
                >
                  申请删除
                </el-button>
                <el-popconfirm
                  v-if="canManage(item) && (item.status === 'draft' || item.status === 'rejected')"
                  title="确定删除该作品？"
                  @confirm="handleDelete(item.id)"
                >
                  <template #reference>
                    <el-button type="danger" link>删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </article>
        </div>

        <div v-else-if="!loading" class="workspace-empty">
          <el-empty description="当前筛选条件下暂无作品" />
        </div>

        <div v-if="total > 0" class="workspace-pagination">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :total="total"
            layout="prev, pager, next, total"
            @current-change="reload"
          />
        </div>
      </div>
    </section>

    <!-- 驳回原因抽屉 -->
    <el-drawer v-model="drawerVisible" title="驳回原因" size="400px">
      <p>{{ rejectReason }}</p>
    </el-drawer>

    <!-- 选择评分批次弹窗 -->
    <el-dialog v-model="batchDialogVisible" title="选择评分批次" width="520px" destroy-on-close>
      <p style="margin-bottom: 16px; font-size: 14px; color: var(--text-secondary)">
        请选择你要提交作品的目标评分批次：
      </p>
      <div v-if="batchLoading" v-loading="batchLoading" style="min-height: 80px" />
      <template v-else>
        <el-radio-group v-model="selectedBatchId" class="batch-radio-group">
          <el-radio v-for="batch in batchList" :key="batch.id" :value="batch.id" class="batch-radio-item">
            <div class="batch-radio-content">
              <strong>{{ batch.batchName }}</strong>
              <span class="batch-radio-time">
                {{ batch.startTime?.replace('T', ' ') }} ~ {{ batch.endTime?.replace('T', ' ') }}
              </span>
            </div>
          </el-radio>
        </el-radio-group>
      </template>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedBatchId" @click="confirmBatch"> 确定，去提交作品 </el-button>
      </template>
    </el-dialog>

    <!-- 申请删除作品弹窗 -->
    <el-dialog v-model="deleteRequestVisible" title="申请删除作品" width="480px" destroy-on-close>
      <p style="margin-bottom: 12px; font-size: 14px; color: var(--text-secondary)">
        作品《{{ deleteRequestWork?.title }}》将被标记为删除申请，管理员审批后才会执行删除。
      </p>
      <el-input
        v-model="deleteRequestReason"
        type="textarea"
        :rows="4"
        placeholder="请说明申请删除的原因"
        maxlength="500"
        show-word-limit
      />
      <template #footer>
        <el-button @click="deleteRequestVisible = false">取消</el-button>
        <el-button type="danger" :loading="deleteRequestSubmitting" @click="handleSubmitDeleteRequest">
          提交申请
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getMyWorks, deleteWork, type WorkItem } from '@/api/student/work'
import type { PageResult } from '@/api/workAdapter'
import { getAvailableBatches, type BatchItem } from '@/api/student/task'
import { useAuthStore } from '@/app/session/auth'
import { useApiList } from '@/composables/useApiList'

const router = useRouter()
const authStore = useAuthStore()
const page = ref(1)
const pageSize = ref(10)
const filterStatus = ref('')
const { loading, list, total, loadList } = useApiList<WorkItem, Parameters<typeof getMyWorks>[0], PageResult<WorkItem>>(
  getMyWorks,
  (data) => ({
    records: data?.records || [],
    total: data?.total || 0,
  }),
)
const reload = () => loadList({ page: page.value, pageSize: pageSize.value, status: filterStatus.value || undefined })

const drawerVisible = ref(false)
const rejectReason = ref('')

const deleteRequestVisible = ref(false)
const deleteRequestSubmitting = ref(false)
const deleteRequestWork = ref<WorkItem | null>(null)
const deleteRequestReason = ref('')

// 批次选择弹窗
const batchDialogVisible = ref(false)
const batchLoading = ref(false)
const batchList = ref<BatchItem[]>([])
const selectedBatchId = ref<number | null>(null)

async function handleCreateWork() {
  batchLoading.value = true
  batchDialogVisible.value = true
  try {
    const res = await getAvailableBatches()
    const list: BatchItem[] = res.data || []
    if (list.length === 0) {
      ElMessage.warning('当前暂无活跃的评分批次')
      batchDialogVisible.value = false
      return
    }
    if (list.length === 1) {
      // 只有一个批次，直接跳转
      batchDialogVisible.value = false
      router.push(`/student/works/create?batchId=${list[0].id}`)
      return
    }
    // 多个批次，展示选择弹窗
    batchList.value = list
    selectedBatchId.value = null
  } catch {
    batchDialogVisible.value = false
  } finally {
    batchLoading.value = false
  }
}

function confirmBatch() {
  if (!selectedBatchId.value) return
  batchDialogVisible.value = false
  router.push(`/student/works/create?batchId=${selectedBatchId.value}`)
}

function openDeleteRequest(item: WorkItem) {
  deleteRequestWork.value = item
  deleteRequestReason.value = ''
  deleteRequestVisible.value = true
}

async function handleSubmitDeleteRequest() {
  if (!deleteRequestReason.value.trim()) {
    ElMessage.warning('请填写申请原因')
    return
  }
  if (!deleteRequestWork.value) return
  deleteRequestSubmitting.value = true
  try {
    const { submitDeleteRequest } = await import('@/api/student/deleteRequest')
    await submitDeleteRequest(deleteRequestWork.value.id, deleteRequestReason.value)
    ElMessage.success('删除申请已提交，等待管理员审批')
    deleteRequestVisible.value = false
  } catch {
    // handled by interceptor
  } finally {
    deleteRequestSubmitting.value = false
  }
}

const statusStats = computed(() => ({
  draft: list.value.filter((item) => item.status === 'draft').length,
  submitted: list.value.filter((item) => item.status === 'submitted').length,
  rejected: list.value.filter((item) => item.status === 'rejected').length,
  approved: list.value.filter((item) => item.status === 'approved').length,
}))

function getDetailRoute(id: string | number) {
  return `/student/works/view/${id}`
}

function canManage(item: WorkItem) {
  return item.submitterId === authStore.userInfo?.id
}

function handleFilterChange() {
  page.value = 1
  reload()
}

async function handleDelete(id: string | number) {
  try {
    await deleteWork(id)
    ElMessage.success('删除成功')
    reload()
  } catch {
    // 错误已在拦截器中处理
  }
}

onMounted(reload)
</script>

<style scoped>
.my-works {
  max-width: 1120px;
  margin: 0 auto;
}

.batch-radio-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.batch-radio-item {
  display: flex;
  align-items: center;
  width: 100%;
  margin-right: 0;
}

.batch-radio-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-left: 8px;
}

.batch-radio-time {
  font-size: 12px;
  color: var(--text-muted, #909399);
}
</style>
