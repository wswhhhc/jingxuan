<template>
  <div class="score-page">
    <section class="editorial-note reveal-up">
      当前页面保持匿名评审，不展示学生姓名和成员信息。建议先浏览简介、附件、视频与运行说明，再进行维度评分。
    </section>

    <section class="filter-panel reveal-up reveal-delay-1">
      <div class="section-heading">
        <div>
          <h2 class="section-heading__title">筛选评审对象</h2>
          <p class="section-heading__meta">按批次、作品名称、技术栈和评分状态快速切换待评材料。</p>
        </div>
      </div>
      <el-form :model="query" inline class="score-filters">
        <el-form-item label="批次">
          <el-select v-model="query.batchId" clearable placeholder="全部批次" style="width: 200px" @change="loadList">
            <el-option v-for="b in batches" :key="b.id" :label="b.batchName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="作品名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="技术栈">
          <el-input v-model="query.techStack" placeholder="技术栈" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="query.onlyUnscored">仅看未评分</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="score-layout reveal-up reveal-delay-2">
      <div class="score-left">
        <section class="table-panel">
          <div class="section-heading">
            <div>
              <h2 class="section-heading__title">作品评分列表</h2>
              <p class="section-heading__meta">共 {{ total }} 项，点击左侧作品进入右侧评分区。</p>
            </div>
          </div>

          <el-table
            :data="workList"
            v-loading="loading"
            row-key="id"
            :current-row-key="selectedWork?.id"
            highlight-current-row
            @row-click="selectWork"
          >
            <template #empty>
              <el-empty :description="loading ? '加载中...' : '暂无作品数据'" />
            </template>
            <el-table-column label="编号" width="76">
              <template #default="{ $index }">
                {{ getRowIndex($index) }}
              </template>
            </el-table-column>
            <el-table-column prop="title" label="作品名称" min-width="220" show-overflow-tooltip />
            <el-table-column prop="techStack" label="技术栈" min-width="150" show-overflow-tooltip />
            <el-table-column label="评分状态" width="108">
              <template #default="{ row }">
                <el-tag v-if="row.scored" type="success" size="small">已评</el-tag>
                <el-tag v-else type="warning" size="small">待评</el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="query.page"
              v-model:page-size="query.size"
              :total="total"
              layout="total, prev, pager, next"
              size="small"
              @change="loadList"
            />
          </div>
        </section>
      </div>

      <!-- 右侧：当前评审信息 -->
      <section v-if="selectedWork" class="surface-panel review-sidebar">
        <div class="section-heading" style="margin-bottom: 14px">
          <div>
            <span class="material-card__kicker">Current Review</span>
            <h2 class="section-heading__title" style="font-size: 18px; margin-top: 4px">{{ selectedWork.title }}</h2>
            <p class="section-heading__meta">评审编号 #{{ selectedWork.id }}</p>
          </div>
        </div>

        <div class="review-metrics">
          <div class="review-metric">
            <span class="review-metric__label">评分状态</span>
            <el-tag v-if="selectedWorkScored" type="success" size="small" effect="dark">已评</el-tag>
            <el-tag v-else type="warning" size="small" effect="dark">待评</el-tag>
          </div>
          <div class="review-metric">
            <span class="review-metric__label">附件数</span>
            <span class="review-metric__value">{{ selectedWork.attachments?.length || 0 }}</span>
          </div>
          <div class="review-metric">
            <span class="review-metric__label">技术栈</span>
            <span class="review-metric__value">{{ selectedWork.techStack || '-' }}</span>
          </div>
          <div class="review-metric">
            <span class="review-metric__label">总分</span>
            <span v-if="selectedWorkScored" class="review-metric__value review-metric__value--brand">{{
              totalScore
            }}</span>
            <span v-else class="review-metric__value">-</span>
          </div>
        </div>

        <div class="review-actions">
          <el-button v-if="selectedWork.previewUrl" size="small" @click="openPreviewDialog">在线预览</el-button>
          <el-button
            v-if="selectedWork.videoUrl"
            size="small"
            type="success"
            @click="openVideoPlayer(selectedWork.videoUrl, '演示视频')"
            >演示视频</el-button
          >
          <el-button v-if="selectedWorkScored" size="small" type="default" @click="scrollToForm">修改评分</el-button>
        </div>
      </section>
    </div>

    <!-- 全宽详情区：材料卡片 / 附件 / 评分表单 -->
    <div v-loading="detailLoading" element-loading-text="加载作品详情..." class="score-detail reveal-up reveal-delay-3">
      <template v-if="selectedWork">
        <!-- 作品材料卡片 -->
        <section class="surface-panel material-card">
          <div class="material-card__header">
            <div>
              <span class="material-card__kicker">Anonymous Review</span>
              <h2 class="material-card__title">{{ selectedWork.title }}</h2>
              <p class="material-card__meta">评分前可直接查看作品材料，不展示学生姓名、成员与提交人信息。</p>
            </div>
            <div class="material-actions">
              <el-button v-if="selectedWork.previewUrl" size="small" @click="openPreviewDialog">在线预览</el-button>
              <el-button
                v-if="selectedWork.videoUrl"
                size="small"
                type="success"
                @click="openVideoPlayer(selectedWork.videoUrl, '演示视频')"
                >演示视频</el-button
              >
            </div>
          </div>

          <div class="material-body">
            <!-- 简介 -->
            <div class="material-section">
              <span class="material-section__label">作品简介</span>
              <p class="material-section__text">{{ selectedWork.summary || '暂无简介' }}</p>
            </div>

            <!-- 技术栈 -->
            <div class="material-section">
              <span class="material-section__label">技术栈</span>
              <div class="tag-list">
                <el-tag v-for="tag in techStackList" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
                <span v-if="techStackList.length === 0" class="text-muted">暂无技术栈信息</span>
              </div>
            </div>

            <!-- 运行说明（有内容时才显示） -->
            <div v-if="selectedWork.runDesc" class="material-section">
              <span class="material-section__label">运行说明</span>
              <pre class="run-desc">{{ selectedWork.runDesc }}</pre>
            </div>
          </div>
        </section>

        <!-- 附件区（全宽卡片） -->
        <section v-if="selectedWork.attachments?.length" class="surface-panel">
          <div class="section-heading">
            <div>
              <h2 class="section-heading__title">附件与媒体</h2>
            </div>
            <span class="section-heading__meta">{{ selectedWork.attachments.length }} 个文件</span>
          </div>

          <!-- 媒体略缩图（图片 + 视频） -->
          <div v-if="mediaAttachments.length" class="image-gallery">
            <div class="image-gallery__grid">
              <template v-for="(media, idx) in mediaAttachments" :key="media.id || idx">
                <!-- 图片：el-image 预览 -->
                <el-image
                  v-if="IMAGE_TYPES.includes(media.fileType?.toLowerCase?.())"
                  :src="media.fileUrl"
                  :preview-src-list="previewImages"
                  :initial-index="previewImages.indexOf(media.fileUrl)"
                  fit="cover"
                  class="gallery-thumb"
                  hide-on-click-modal
                  preview-teleported
                >
                  <template #error>
                    <div class="gallery-thumb-error">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <!-- 视频：首帧封面图 -->
                <div
                  v-else
                  class="gallery-thumb gallery-thumb--video"
                  @click="openVideoPlayer(media.fileUrl, media.fileName)"
                  :title="media.fileName"
                >
                  <video
                    :src="media.fileUrl"
                    preload="metadata"
                    muted
                    playsinline
                    class="gallery-thumb-video-el"
                  ></video>
                  <div class="gallery-thumb-video-overlay">
                    <el-icon :size="28"><VideoCamera /></el-icon>
                  </div>
                </div>
              </template>
            </div>
          </div>

          <!-- 附件文件列表 -->
          <div class="attachments-header">
            <span class="attachments-count">共 {{ selectedWork.attachments.length }} 个文件</span>
            <el-button link type="primary" size="small" @click="toggleAttachmentExpand">
              {{ attachmentExpanded ? '收起' : '展开全部' }}
            </el-button>
          </div>
          <div v-for="att in visibleAttachments" :key="att.id || att.fileName" class="attachment-item">
            <div class="attachment-item__left">
              <el-icon><component :is="attachmentIcon(att.fileType)" /></el-icon>
              <span>{{ att.fileName }}</span>
            </div>
            <el-button link type="primary" @click="openLink(att.fileUrl)">查看附件</el-button>
          </div>
          <div v-if="attachmentExpanded && totalAttachmentPages > 1" class="attachment-pagination">
            <el-pagination
              v-model:current-page="attachmentPage"
              :page-size="PAGE_SIZE"
              :total="selectedWork.attachments.length"
              layout="prev, pager, next"
              size="small"
            />
          </div>
        </section>

        <!-- 视频播放弹窗 -->
        <el-dialog
          v-model="videoDialogVisible"
          :title="activeVideoName"
          width="75%"
          top="4vh"
          destroy-on-close
          class="video-player-dialog"
          append-to-body
        >
          <video :src="activeVideoUrl" controls autoplay class="video-player"></video>
        </el-dialog>

        <!-- 评分表单 -->
        <section class="surface-panel score-card">
          <div class="section-heading">
            <div>
              <h2 class="section-heading__title">评分表单</h2>
              <p class="section-heading__meta">
                按维度打分后自动汇总总分。可使用 <kbd class="key-hint">↑</kbd
                ><kbd class="key-hint">↓</kbd> 切换作品，<kbd class="key-hint">Ctrl+Enter</kbd> 快速提交。
              </p>
            </div>
            <div class="score-total-preview">
              <span class="score-total-preview__num">{{ totalScore }}</span>
              <span class="score-total-preview__unit">分</span>
            </div>
          </div>

          <form class="score-dimensions" @submit.prevent="handleSubmit">
            <div v-for="dim in DIMENSIONS" :key="dim.key" class="score-dim-item">
              <div class="score-dim__header">
                <span class="score-dim__label">{{ dim.label }}</span>
                <span class="score-dim__value">{{ scoreForm[dim.key] }}</span>
              </div>
              <el-slider
                v-model="scoreForm[dim.key]"
                :min="dim.min"
                :max="dim.max"
                show-input
                :show-input-controls="false"
                :input-size="'small'"
              />
              <div class="score-dim__footer">
                <span>{{ dim.min }}</span
                ><span>{{ dim.max }}</span>
              </div>
            </div>
          </form>

          <div class="score-foot">
            <div class="score-comment">
              <el-input
                v-model="scoreForm.comment"
                type="textarea"
                :rows="2"
                placeholder="填写评语（可选）"
                maxlength="500"
                show-word-limit
              />
            </div>
            <div class="score-submit-row">
              <div class="score-submit__total">
                总分：<strong>{{ totalScore }}</strong> 分
              </div>
              <el-button type="primary" :loading="submitting" size="large" @click="handleSubmit">
                {{ selectedWorkScored ? '更新评分' : '提交评分' }}
              </el-button>
            </div>
          </div>
        </section>
      </template>

      <section v-else class="surface-panel score-detail__empty">
        <div class="section-heading">
          <div>
            <h2 class="section-heading__title">评分提示</h2>
            <p class="section-heading__meta">请从左侧列表选择一个作品开始评分。</p>
          </div>
        </div>
        <p class="text-muted tip-list">
          1. 当前页面保持匿名评审，不展示学生姓名和成员信息。<br />
          2. 可先查看简介、附件、视频、预览与运行说明后再打分。<br />
          3. 评分截止前可返回"我的评分记录"继续调整。
        </p>
      </section>
    </div>
  </div>

  <PreviewDialog ref="previewDialog" :url="previewUrl" />
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Files, Document, VideoCamera } from '@element-plus/icons-vue'
import { getScoredWorkList } from '@/api/teacher/work'
import { submitScore, getMyScore, getBatchList } from '@/api/teacher/score'
import { getWorkDetail } from '@/api/teacher/work'
import type { WorkDetailVO, WorkListVO } from '@/api/types'
import { IMAGE_TYPES, VIDEO_TYPES, MEDIA_TYPES } from '@/api/types'
import PreviewDialog from '@/components/PreviewDialog.vue'

// ===== 评分维度常量 =====
const DIMENSIONS = [
  { key: 'innovation' as const, label: '创新性', min: 0, max: 25, default: 15 },
  { key: 'difficulty' as const, label: '技术难度', min: 0, max: 25, default: 15 },
  { key: 'completion' as const, label: '完成度', min: 0, max: 30, default: 20 },
  { key: 'practicality' as const, label: '实用性', min: 0, max: 20, default: 12 },
] as const

type DimKey = (typeof DIMENSIONS)[number]['key']

function getDefaults(): Record<DimKey, number> {
  const obj = {} as Record<DimKey, number>
  DIMENSIONS.forEach((d) => {
    obj[d.key] = d.default
  })
  return obj
}

// ===== 路由 & 状态 =====
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detailLoading = ref(false)
const submitting = ref(false)
const isMounted = ref(false)

const workList = ref<WorkListVO[]>([])
const batches = ref<{ id: number; batchName: string }[]>([])
const total = ref(0)
const selectedWork = ref<WorkDetailVO | null>(null)
const selectedWorkScored = ref(false)
const previewDialog = ref<InstanceType<typeof PreviewDialog> | null>(null)
const previewUrl = ref('')

/** 追踪当前选中的作品 ID，用于防止异步竞态 */
const lastSelectedId = ref<number | null>(null)

interface ScoreForm {
  innovation: number
  difficulty: number
  completion: number
  practicality: number
  comment: string
}

const query = reactive({
  page: 1,
  size: 5,
  batchId: undefined as number | undefined,
  keyword: '',
  techStack: '',
  onlyUnscored: false,
})

const scoreDefaults = getDefaults()

const scoreForm = reactive<ScoreForm>({
  innovation: scoreDefaults.innovation,
  difficulty: scoreDefaults.difficulty,
  completion: scoreDefaults.completion,
  practicality: scoreDefaults.practicality,
  comment: '',
})
const totalScore = computed(() => DIMENSIONS.reduce((sum, d) => sum + (scoreForm[d.key] || 0), 0))

const techStackList = computed(() =>
  (selectedWork.value?.techStack || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean),
)

// ===== 附件 =====
const attachmentPage = ref(1)
const PAGE_SIZE = 6
const attachmentExpanded = ref(false)
const videoDialogVisible = ref(false)
const activeVideoUrl = ref('')
const activeVideoName = ref('')

/** 媒体附件（图片+视频），用于画廊网格展示 */
const mediaAttachments = computed(() => {
  return selectedWork.value?.attachments?.filter((att) => MEDIA_TYPES.includes(att.fileType?.toLowerCase?.())) || []
})

/** 可预览图片列表（封面 + 图片附件），供 el-image 预览使用 */
const previewImages = computed(() => {
  const list: string[] = []
  if (selectedWork.value?.coverUrl) list.push(selectedWork.value.coverUrl)
  selectedWork.value?.attachments?.forEach((att) => {
    if (IMAGE_TYPES.includes(att.fileType?.toLowerCase?.()) && att.fileUrl && !list.includes(att.fileUrl)) {
      list.push(att.fileUrl)
    }
  })
  return list
})

const visibleAttachments = computed(() => {
  if (!selectedWork.value?.attachments) return []
  if (!attachmentExpanded.value) return []
  const start = (attachmentPage.value - 1) * PAGE_SIZE
  return selectedWork.value.attachments.slice(start, start + PAGE_SIZE)
})

const totalAttachmentPages = computed(() => {
  return Math.ceil((selectedWork.value?.attachments?.length || 0) / PAGE_SIZE)
})

function toggleAttachmentExpand() {
  attachmentExpanded.value = !attachmentExpanded.value
  if (!attachmentExpanded.value) attachmentPage.value = 1
}

function attachmentIcon(fileType: string): Component {
  const t = (fileType || '').toLowerCase()
  if (IMAGE_TYPES.includes(t)) return Picture
  if (VIDEO_TYPES.includes(t)) return VideoCamera
  if (t === 'pdf' || t === 'doc' || t === 'docx') return Document
  return Files
}

// ===== 数据加载 =====
const getRowIndex = (index: number) => (query.page - 1) * query.size + index + 1

const resetScoreForm = () => {
  scoreForm.innovation = scoreDefaults.innovation
  scoreForm.difficulty = scoreDefaults.difficulty
  scoreForm.completion = scoreDefaults.completion
  scoreForm.practicality = scoreDefaults.practicality
  scoreForm.comment = ''
  attachmentExpanded.value = false
  attachmentPage.value = 1
}

const buildFallbackWork = (row: Partial<WorkListVO> & { id: number; batchId?: number }): WorkDetailVO => ({
  id: row.id,
  title: row.title || `作品 #${row.id}`,
  summary: '',
  techStack: row.techStack || '',
  advisor: '',
  coverUrl: '',
  videoUrl: '',
  runDesc: '',
  status: 3,
  statusLabel: '',
  submitterId: 0,
  submitterName: '',
  submitTime: '',
  batchId: row.batchId || 0,
  publishStatus: 0,
  publishStatusLabel: '',
  featured: 0,
  previewUrl: '',
  avgScore: '',
  members: [],
  attachments: [],
})

const loadBatches = async () => {
  try {
    const res = await getBatchList()
    batches.value = res.data || []
  } catch {
    batches.value = []
    ElMessage.error('加载批次列表失败')
  }
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await getScoredWorkList({ ...query })
    workList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {
    ElMessage.error('加载作品列表失败')
  } finally {
    loading.value = false
  }
}

const loadMyScoreData = async (workId: number) => {
  try {
    const scoreRes = await getMyScore(workId)
    if (scoreRes.data) {
      scoreForm.innovation = Number(scoreRes.data.innovation ?? scoreDefaults.innovation)
      scoreForm.difficulty = Number(scoreRes.data.difficulty ?? scoreDefaults.difficulty)
      scoreForm.completion = Number(scoreRes.data.completion ?? scoreDefaults.completion)
      scoreForm.practicality = Number(scoreRes.data.practicality ?? scoreDefaults.practicality)
      scoreForm.comment = scoreRes.data.comment || ''
      selectedWorkScored.value = true
      return
    }
  } catch {
    // 未评分属于正常情况（接口返回 404 或空数据），不报错
  }
  selectedWorkScored.value = false
  resetScoreForm()
}

const loadSelectedWork = async (row: Partial<WorkListVO> & { id: number; batchId?: number }) => {
  attachmentExpanded.value = false
  attachmentPage.value = 1
  try {
    const detailRes = await getWorkDetail(row.id)
    selectedWork.value = detailRes.data
  } catch {
    selectedWork.value = buildFallbackWork(row)
    ElMessage.error('加载作品详情失败，使用部分信息')
  }
  await loadMyScoreData(row.id)
}

const clearScoreRouteQuery = () => {
  if (!route.query.workId && !route.query.batchId && !route.query.from) return
  router.replace({ path: route.path }).catch(() => undefined)
}

// ===== 作品切换 =====
const selectWork = async (row: WorkListVO) => {
  const id = row.id
  lastSelectedId.value = id
  detailLoading.value = true

  await loadSelectedWork(row)

  if (lastSelectedId.value !== id) {
    detailLoading.value = false
    return // 过期响应，丢弃
  }

  detailLoading.value = false
  clearScoreRouteQuery()
}

const syncRouteSelection = async () => {
  const rawWorkId = Array.isArray(route.query.workId) ? route.query.workId[0] : route.query.workId
  const rawBatchId = Array.isArray(route.query.batchId) ? route.query.batchId[0] : route.query.batchId
  const rawFrom = Array.isArray(route.query.from) ? route.query.from[0] : route.query.from
  const workId = Number(rawWorkId)
  if (!workId) return

  if (rawFrom === 'history' && query.onlyUnscored) {
    query.onlyUnscored = false
  }
  if (rawBatchId && query.batchId !== Number(rawBatchId)) {
    query.batchId = Number(rawBatchId)
  }

  const matched = workList.value.find((item) => item.id === workId)
  if (matched) {
    await loadSelectedWork(matched)
  } else {
    await loadSelectedWork({ id: workId, batchId: Number(rawBatchId) || undefined })
  }
  clearScoreRouteQuery()
}

// ===== 操作 =====
const handleSearch = async () => {
  query.page = 1
  await loadList()
}

const handleReset = async () => {
  query.page = 1
  query.keyword = ''
  query.techStack = ''
  query.batchId = undefined
  query.onlyUnscored = false
  await loadList()
  clearScoreRouteQuery()
}

const handleSubmit = async () => {
  if (!selectedWork.value) return

  const total = totalScore.value
  if (total === 0) {
    ElMessage.warning('请调整各维度分数后再提交')
    return
  }

  try {
    await ElMessageBox.confirm(`确定${selectedWorkScored.value ? '更新' : '提交'}评分（${total} 分）？`, '确认评分', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return // 取消操作
  }

  submitting.value = true
  try {
    await submitScore({
      workId: selectedWork.value.id,
      innovation: scoreForm.innovation,
      difficulty: scoreForm.difficulty,
      completion: scoreForm.completion,
      practicality: scoreForm.practicality,
      comment: scoreForm.comment,
    })
    ElMessage.success(selectedWorkScored.value ? '评分已更新' : '评分提交成功')
    selectedWorkScored.value = true
    await loadList()
  } catch {
    ElMessage.error('评分提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

const openLink = (url?: string) => {
  if (!url) return
  window.open(url, '_blank')
}

function openPreviewDialog() {
  if (!selectedWork.value?.previewUrl) return
  previewUrl.value = selectedWork.value.previewUrl
  previewDialog.value?.open()
}

const openVideoPlayer = (url: string, name: string) => {
  activeVideoUrl.value = url
  activeVideoName.value = name
  videoDialogVisible.value = true
}

const scrollToForm = () => {
  const el = document.querySelector('.score-card')
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// ===== 键盘快捷键 =====
const handleKeydown = (e: KeyboardEvent) => {
  // Ctrl+Enter / Cmd+Enter → 提交评分
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    handleSubmit()
    return
  }

  // 如果焦点在输入框中，不拦截方向键
  const tag = document.activeElement?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || (document.activeElement as HTMLElement)?.isContentEditable) {
    return
  }

  // ↑/↓ → 切换作品
  if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
    e.preventDefault()
    const dir = e.key === 'ArrowDown' ? 1 : -1
    const currentIndex = workList.value.findIndex((w) => w.id === selectedWork.value?.id)
    const nextIndex = currentIndex + dir
    if (nextIndex >= 0 && nextIndex < workList.value.length) {
      selectWork(workList.value[nextIndex])
    }
  }
}

// ===== 生命周期 =====
onMounted(async () => {
  const rawBatchId = Array.isArray(route.query.batchId) ? route.query.batchId[0] : route.query.batchId
  const rawFrom = Array.isArray(route.query.from) ? route.query.from[0] : route.query.from
  const batchId = Number(rawBatchId)
  if (batchId) query.batchId = batchId
  if (rawFrom === 'history') query.onlyUnscored = false

  // 独立请求并行加载
  await Promise.all([loadBatches(), loadList()])
  await syncRouteSelection()

  isMounted.value = true
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.score-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.score-filters :deep(.el-form-item) {
  margin-bottom: 0;
}

.score-layout {
  /* 顶部：评分列表 + 评审信息 双列布局 */
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(380px, 0.9fr);
  gap: 24px;
  align-items: start;
}

.score-left {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 全宽详情区：材料卡片 / 附件 / 评分表单 各占一行 */
.score-detail {
  display: flex;
  flex-direction: column;
  gap: 22px;
  min-height: 300px;
}

/* ===== 键盘提示 ===== */
.key-hint {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border: 1px solid var(--border-subtle);
  border-radius: 4px;
  font-size: 11px;
  font-family: inherit;
  color: var(--text-muted);
  background: color-mix(in srgb, var(--card-bg) 70%, transparent);
  vertical-align: middle;
}

/* ===== 材料卡片 ===== */
.material-card__header {
  margin-bottom: 20px;
}
.material-card__kicker {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  color: var(--text-muted);
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}
.material-card__kicker::before {
  content: '';
  width: 28px;
  height: 1px;
  background: linear-gradient(90deg, var(--brand), transparent);
}
.material-card__title {
  margin: 0 0 8px;
  font-family: var(--font-display);
  font-size: 26px;
  line-height: 1.1;
}
.material-card__meta {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.7;
}
.material-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}
.material-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.material-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.material-section__label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  letter-spacing: 0.06em;
  text-transform: uppercase;
}
.material-section__text {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.85;
}
.text-muted {
  color: var(--text-muted);
}
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.run-desc {
  margin: 0;
  padding: 14px;
  border-radius: 16px;
  background: color-mix(in srgb, var(--page-bg) 60%, transparent);
  color: var(--text-secondary);
  white-space: pre-wrap;
  line-height: 1.8;
  font-family: inherit;
}

/* ===== 附件 ===== */
.attachments-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.attachments-count {
  font-size: 13px;
  color: var(--text-muted);
}
.attachment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 8px;
  border: 1px solid var(--border-subtle);
  border-radius: 16px;
  background: color-mix(in srgb, var(--card-bg) 82%, transparent);
}
.attachment-item:last-child {
  margin-bottom: 0;
}
.attachment-item__left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.attachment-item__left span {
  word-break: break-all;
  font-size: 13px;
}
.attachment-pagination {
  display: flex;
  justify-content: center;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--border-subtle);
}

/* ===== 图片略缩图 ===== */
.image-gallery {
  margin-bottom: 20px;
}
.image-gallery__grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.gallery-thumb {
  width: 90px;
  height: 90px;
  border-radius: 12px;
  cursor: pointer;
  object-fit: cover;
  border: 1px solid var(--border-subtle);
  transition:
    transform 0.25s ease,
    box-shadow 0.25s ease;
  flex-shrink: 0;
}
.gallery-thumb:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.14);
}
.gallery-thumb-error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--text-muted);
  background: var(--page-bg);
  border-radius: 12px;
}

/* ===== 视频略缩图 ===== */
.gallery-thumb--video {
  position: relative;
  overflow: hidden;
  background: #000;
  cursor: pointer;
}
.gallery-thumb-video-el {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.gallery-thumb-video-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.3);
  color: #fff;
  transition: background 0.2s;
}
.gallery-thumb--video:hover .gallery-thumb-video-overlay {
  background: rgba(0, 0, 0, 0.5);
}
.gallery-thumb--video:hover .gallery-thumb-video-overlay .el-icon {
  transform: scale(1.15);
}
.gallery-thumb--video .gallery-thumb-video-overlay .el-icon {
  transition: transform 0.2s;
}

/* 视频播放弹窗 */
.video-player-dialog :deep(.el-dialog__body) {
  padding: 0;
  background: #000;
  border-radius: 0 0 8px 8px;
}
.video-player {
  display: block;
  width: 100%;
  max-height: 80vh;
  outline: none;
}

/* ===== 评分表单 ===== */
.score-card .section-heading {
  margin-bottom: 20px;
}
.score-total-preview {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.score-total-preview__num {
  font-family: var(--font-display);
  font-size: 34px;
  line-height: 0.9;
  color: var(--brand);
}
.score-total-preview__unit {
  font-size: 14px;
  color: var(--text-muted);
}

.score-dimensions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-bottom: 20px;
}

.score-dim-item {
  padding: 16px 18px;
  border: 1px solid var(--border-subtle);
  border-radius: 16px;
  background: color-mix(in srgb, var(--card-bg) 82%, transparent);
}

.score-dim__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.score-dim__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  letter-spacing: 0.04em;
}
.score-dim__value {
  font-family: var(--font-display);
  font-size: 22px;
  line-height: 1;
  color: var(--text-primary);
}
.score-dim__footer {
  display: flex;
  justify-content: space-between;
  margin-top: 4px;
  font-size: 11px;
  color: var(--text-muted);
}

.score-dim-item :deep(.el-slider) {
  --el-slider-runway-bg-color: var(--border-subtle);
  --el-slider-stop-bg-color: var(--border-color);
  --el-slider-button-size: 20px;
  padding: 0 2px;
}
.score-dim-item :deep(.el-slider__bar) {
  background: linear-gradient(90deg, var(--brand), color-mix(in srgb, var(--brand) 70%, var(--accent)));
}
.score-dim-item :deep(.el-slider__button) {
  border-color: color-mix(in srgb, var(--brand) 60%, transparent);
  box-shadow: 0 2px 8px color-mix(in srgb, var(--brand) 24%, transparent);
}
/* slider 内联输入框样式 */
.score-dim-item :deep(.el-slider__input) {
  width: 50px;
}
.score-dim-item :deep(.el-slider__input .el-input__inner) {
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 600;
  height: 28px;
  line-height: 28px;
  text-align: center;
}

/* ===== 底部提交区 ===== */
.score-foot {
  border-top: 1px solid var(--border-subtle);
  padding-top: 18px;
}
.score-comment {
  margin-bottom: 14px;
}
.score-submit-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.score-submit__total {
  font-size: 15px;
  color: var(--text-secondary);
}
.score-submit__total strong {
  font-family: var(--font-display);
  font-size: 28px;
  color: var(--brand);
  margin: 0 4px;
}

/* ===== 空状态 ===== */
.tip-list {
  margin-top: 8px;
}

/* ===== 左侧评审侧栏 ===== */
.review-sidebar {
  padding: 20px 22px;
}
.review-metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}
.review-metric {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  border: 1px solid var(--border-subtle);
  border-radius: 14px;
  background: color-mix(in srgb, var(--card-bg) 78%, transparent);
}
.review-metric__label {
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 0.06em;
  text-transform: uppercase;
}
.review-metric__value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.review-metric__value--brand {
  font-family: var(--font-display);
  font-size: 22px;
  color: var(--brand);
}
.review-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* ===== 响应式 ===== */
@media (max-width: 1280px) {
  .score-dimensions {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 1080px) {
  .score-layout {
    grid-template-columns: 1fr;
  }
  .score-dimensions {
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 640px) {
  .score-dimensions {
    grid-template-columns: 1fr;
  }
  .score-submit-row {
    flex-direction: column;
    align-items: stretch;
  }
  .score-submit__total {
    text-align: center;
  }
}
</style>
