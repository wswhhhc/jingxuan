<template>
  <div class="app-page work-detail" v-loading="loading">
    <div v-if="work" class="detail-stack">
      <!-- ← 返回 -->
      <div class="detail-back">
        <el-button link @click="router.push('/works')">← 返回作品展廊</el-button>
      </div>

      <!-- ====== HERO ====== -->
      <section class="surface-panel detail-hero reveal-up">
        <div class="detail-hero__meta">
          <span class="page-kicker">Work Profile</span>
          <h1 class="detail-hero__title">{{ work.title }}</h1>
          <div class="detail-hero__info">
            <span class="info-dot">作者：{{ work.submitterName }}</span>
            <span class="info-dot">指导教师：{{ work.advisor || '未标注' }}</span>
            <span>提交时间：{{ work.submitTime }}</span>
          </div>
          <div class="detail-hero__tags" v-if="work.tags?.length">
            <el-tag v-for="tag in work.tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
          </div>

          <p class="detail-hero__summary">{{ work.summary || '暂无作品简介。' }}</p>

          <div class="detail-hero__actions">
            <div class="hero-stats">
              <button class="hero-stat-btn" :class="{ 'is-liked': liked }" @click="handleLike">
                <el-icon :size="16"><StarFilled /></el-icon>
                <span>{{ likeCount }}</span>
              </button>
              <span class="hero-stat-line">
                <el-icon :size="16"><View /></el-icon>
                <span>{{ viewCount }}</span>
              </span>
            </div>
            <div class="hero-actions-right">
              <el-tag v-if="work.featured" type="warning" effect="dark">精选作品</el-tag>
              <el-button v-if="work.previewUrl" type="primary" size="small" @click="openPreview">在线体验</el-button>
            </div>
          </div>
        </div>

        <div class="detail-hero__cover">
          <el-image :src="work.coverUrl || DEFAULT_COVER_URL" fit="contain" class="detail-cover">
            <template #error>
              <div class="cover-placeholder">
                <el-icon :size="48"><Picture /></el-icon>
              </div>
            </template>
          </el-image>

          <!-- 评分浮标 -->
          <div v-if="work.score !== undefined" class="score-badge">
            <div class="score-badge__num">{{ work.score }}</div>
            <div class="score-badge__label">分</div>
            <div v-if="work.rank" class="score-badge__rank">第 {{ work.rank }} 名</div>
          </div>
        </div>
      </section>

      <!-- ====== 作品信息 + 团队成员 ====== -->
      <div class="detail-grid reveal-up reveal-delay-1">
        <section class="surface-panel work-info-panel" :class="{ 'work-info-panel--expanded': infoExpanded }">
          <div class="section-heading">
            <h2 class="section-heading__title">作品信息</h2>
          </div>
          <div class="work-info-content">
            <div class="info-list">
              <div class="info-item">
                <span class="info-label">技术栈</span>
                <div class="info-value tag-wrap">
                  <el-tag v-for="tag in techTags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
                  <span v-if="techTags.length === 0" class="text-muted">暂无技术栈信息</span>
                </div>
              </div>
              <div class="info-item">
                <span class="info-label">指导教师</span>
                <span class="info-value">{{ work.advisor || '无' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">提交时间</span>
                <span class="info-value">{{ work.submitTime }}</span>
              </div>
            </div>
            <div v-if="work.runDescription" class="detail-block">
              <h3>运行说明</h3>
              <pre class="run-desc">{{ work.runDescription }}</pre>
            </div>
          </div>
          <button v-if="isInfoOverflow" class="info-expand-toggle" @click="infoExpanded = !infoExpanded">
            {{ infoExpanded ? '收起' : '查看详细' }}
          </button>
        </section>

        <section class="surface-panel">
          <div class="section-heading">
            <h2 class="section-heading__title">团队成员</h2>
            <span class="section-heading__meta">{{ work.members?.length || 0 }} 位成员</span>
          </div>
          <div v-if="work.members?.length" class="member-list">
            <div v-for="m in work.members" :key="m.id || m.studentNo" class="member-item">
              <el-avatar :size="36" :src="m.avatar">{{ m.avatar ? '' : m.studentName.charAt(0) }}</el-avatar>
              <div class="member-info">
                <div class="member-name">
                  <span>{{ m.studentName }}</span>
                  <el-tag v-if="m.isLeader" type="warning" size="small" effect="dark">队长</el-tag>
                </div>
                <span class="member-meta">{{ m.studentNo }} · {{ m.className }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无成员信息" :image-size="60" />
        </section>
      </div>

      <!-- ====== 附件与媒体（全宽） ====== -->
      <section class="surface-panel reveal-up reveal-delay-2" v-if="work.attachments?.length">
        <div class="section-heading">
          <h2 class="section-heading__title">附件与媒体</h2>
          <span class="section-heading__meta">{{ work.attachments.length }} 个文件</span>
        </div>

        <!-- 媒体略缩图（图片 + 视频） -->
        <div v-if="mediaAttachments.length" class="image-gallery">
          <div class="image-gallery__grid">
            <template v-for="(media, idx) in mediaAttachments" :key="media.id || idx">
              <el-image
                v-if="IMAGE_TYPES.includes(media.fileType?.toLowerCase?.())"
                :src="media.fileUrl"
                :preview-src-list="previewSrcList"
                :initial-index="previewIndexMap[media.fileUrl] ?? 0"
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
              <div v-else class="gallery-thumb gallery-thumb--video" @click="openVideo(media)" :title="media.fileName">
                <video
                  :src="media.fileUrl"
                  preload="metadata"
                  muted
                  playsinline
                  class="gallery-thumb__video-el"
                ></video>
                <div class="gallery-thumb__video-overlay">
                  <el-icon :size="28"><VideoCamera /></el-icon>
                </div>
              </div>
            </template>
          </div>
        </div>

        <!-- 附件文件列表 + 分页 -->
        <div class="attachments-header">
          <span class="attachments-count">共 {{ work.attachments.length }} 个文件</span>
          <el-button link type="primary" size="small" @click="toggleAttachmentExpand">
            {{ attachmentExpanded ? '收起' : '展开全部' }}
          </el-button>
        </div>
        <div v-for="att in paginatedAttachments" :key="att.id || att.fileName" class="attachment-item">
          <div class="attachment-item__left">
            <el-icon><component :is="attachmentIcon(att.fileType)" /></el-icon>
            <span>{{ att.fileName }}</span>
          </div>
          <el-button link type="primary" @click="downloadFile(att.fileUrl, att.fileName)">下载</el-button>
        </div>
        <div v-if="attachmentExpanded && totalAttachmentPages > 1" class="attachment-pagination">
          <el-pagination
            v-model:current-page="attachmentPage"
            :page-size="pageSize"
            :total="work.attachments.length"
            layout="prev, pager, next"
            size="small"
          />
        </div>
        <p v-if="!work.attachments.length" class="text-muted">暂无附件资料</p>
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

      <!-- ====== 评分详情（全宽，有评分才显示） ====== -->
      <section v-if="work.score !== undefined" class="surface-panel score-detail reveal-up reveal-delay-2">
        <div class="section-heading">
          <div>
            <h2 class="section-heading__title">评分详情</h2>
            <p class="section-heading__meta" v-if="work.teacherCount">{{ work.teacherCount }} 位教师参与评分</p>
          </div>
          <div class="score-detail__summary">
            <span class="score-detail__big">{{ work.score }}</span>
            <span class="score-detail__unit">分</span>
          </div>
        </div>

        <div class="score-dimensions">
          <div class="score-dim">
            <div class="score-dim__header">
              <span class="score-dim__label">创新性</span>
              <span class="score-dim__val">{{ work.avgInnovation || '-' }}</span>
            </div>
            <div class="score-dim__bar">
              <div class="score-dim__fill" :style="{ width: dimPercent(work.avgInnovation, 25) }"></div>
            </div>
            <span class="score-dim__max">/ 25</span>
          </div>
          <div class="score-dim">
            <div class="score-dim__header">
              <span class="score-dim__label">技术难度</span>
              <span class="score-dim__val">{{ work.avgDifficulty || '-' }}</span>
            </div>
            <div class="score-dim__bar">
              <div class="score-dim__fill fill--blue" :style="{ width: dimPercent(work.avgDifficulty, 25) }"></div>
            </div>
            <span class="score-dim__max">/ 25</span>
          </div>
          <div class="score-dim">
            <div class="score-dim__header">
              <span class="score-dim__label">完成度</span>
              <span class="score-dim__val">{{ work.avgCompletion || '-' }}</span>
            </div>
            <div class="score-dim__bar">
              <div class="score-dim__fill fill--green" :style="{ width: dimPercent(work.avgCompletion, 30) }"></div>
            </div>
            <span class="score-dim__max">/ 30</span>
          </div>
          <div class="score-dim">
            <div class="score-dim__header">
              <span class="score-dim__label">实用性</span>
              <span class="score-dim__val">{{ work.avgPracticality || '-' }}</span>
            </div>
            <div class="score-dim__bar">
              <div class="score-dim__fill fill--gold" :style="{ width: dimPercent(work.avgPracticality, 20) }"></div>
            </div>
            <span class="score-dim__max">/ 20</span>
          </div>
        </div>
      </section>

      <!-- ====== 评论 ====== -->
      <section class="surface-panel comment-card reveal-up reveal-delay-3">
        <div class="section-heading">
          <div>
            <h2 class="section-heading__title">作品评论</h2>
            <p class="section-heading__meta">共 {{ commentTotal }} 条交流内容</p>
          </div>
        </div>

        <div v-if="isLoggedIn" class="comment-input">
          <el-input
            v-model="commentText"
            type="textarea"
            :rows="3"
            placeholder="写下你的评论..."
            maxlength="500"
            show-word-limit
          />
          <el-button type="primary" :loading="commentSending" class="comment-submit" @click="submitComment">
            发表评论
          </el-button>
        </div>

        <div v-else class="comment-input comment-input--guest">
          <div class="guest-name-row">
            <el-input
              v-model="guestName"
              placeholder="你的昵称（选填）"
              maxlength="20"
              class="guest-name-input"
              clearable
            />
          </div>
          <el-input
            v-model="commentText"
            type="textarea"
            :rows="3"
            placeholder="写下你的评论..."
            maxlength="500"
            show-word-limit
          />
          <el-button type="primary" :loading="commentSending" class="comment-submit" @click="submitComment">
            发表评论
          </el-button>
        </div>

        <div v-if="comments.length" class="comment-list">
          <CommentThread
            v-for="c in comments"
            :key="c.id"
            :comment="c"
            :depth="0"
            :is-logged-in="isLoggedIn"
            :reply-target-id="replyTargetId"
            :reply-target-name="replyTargetName"
            :reply-sending="replySending"
            :current-user-id="currentUser?.id ?? null"
            :is-admin="isAdmin"
            :is-work-leader="isWorkLeader"
            @open-reply="openReply"
            @cancel-reply="cancelReply"
            @submit-reply="submitReply"
            @delete-comment="handleDeleteComment"
            @go-to-login="goToLogin"
          />
        </div>
        <el-empty v-else description="暂无评论，来抢沙发吧~" :image-size="80" />

        <div v-if="commentTotal > comments.length" class="load-more">
          <el-button link type="primary" @click="loadMoreComments">加载更多</el-button>
        </div>
      </section>
    </div>
  </div>

  <PreviewDialog ref="previewDialog" :url="previewUrl" />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Files, Document, VideoCamera, StarFilled, View } from '@element-plus/icons-vue'
import { getPublicWorkDetail, toggleLike } from '@/api/public/work'
import { getCommentList, addComment, deleteComment } from '@/api/public/comment'
import type { CommentItem } from '@/api/public/comment'
import type { UserInfo } from '@/api/student/auth'
import type { WorkAttachment, WorkItem } from '@/api/student/work'
import { IMAGE_TYPES, VIDEO_TYPES } from '@/api/types'
import { getCachedUserInfo, hasLoginToken } from '@/utils/auth'
import { DEFAULT_COVER_URL } from '@/utils/cover'
import CommentThread from './CommentThread.vue'
import PreviewDialog from '@/components/PreviewDialog.vue'

const route = useRoute()
const router = useRouter()
const previewDialog = ref<InstanceType<typeof PreviewDialog> | null>(null)
const previewUrl = ref('')
const work = ref<WorkItem | null>(null)
const loading = ref(false)
const infoExpanded = ref(false)
const isInfoOverflow = ref(false)
const comments = ref<CommentItem[]>([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentText = ref('')
const commentSending = ref(false)
const replyTargetId = ref<string | number | null>(null)
const replyTargetName = ref('')
const replySending = ref(false)
const currentUser = ref<UserInfo | null>(getCachedUserInfo())
const liked = ref(false)
const likeCount = ref(0)
const viewCount = ref(0)
const guestName = ref('')
const attachmentPage = ref(1)
const pageSize = 6
const attachmentExpanded = ref(false)
const videoDialogVisible = ref(false)
const activeVideoUrl = ref('')
const activeVideoName = ref('')

const techTags = computed(() => {
  const ts = work.value?.techStack
  if (!ts) return []
  if (Array.isArray(ts)) return ts
  return ts
    .split(',')
    .map((t: string) => t.trim())
    .filter(Boolean)
})
const isLoggedIn = ref(hasLoginToken())
const isAdmin = computed(() => currentUser.value?.roleCode === 'ROLE_ADMIN')
const isWorkLeader = computed(() => {
  const userId = currentUser.value?.id
  if (!userId || !work.value?.members?.length) return false
  return work.value.members.some((member) => member.isLeader && member.studentId === userId)
})

const imageAttachments = computed(() => {
  return work.value?.attachments?.filter((att) => IMAGE_TYPES.includes(att.fileType?.toLowerCase?.())) || []
})

const mediaAttachments = computed(() => {
  return (
    work.value?.attachments?.filter((att) => {
      const t = att.fileType?.toLowerCase?.() || ''
      return IMAGE_TYPES.includes(t) || VIDEO_TYPES.includes(t)
    }) || []
  )
})

const previewSrcList = computed(() => {
  const list: string[] = []
  if (work.value?.coverUrl) list.push(work.value.coverUrl)
  imageAttachments.value.forEach((img) => {
    if (img.fileUrl && !list.includes(img.fileUrl)) list.push(img.fileUrl)
  })
  return list
})

const previewIndexMap = computed(() => {
  const map: Record<string, number> = {}
  previewSrcList.value.forEach((url, idx) => {
    map[url] = idx
  })
  return map
})

const paginatedAttachments = computed(() => {
  if (!work.value?.attachments) return []
  if (!attachmentExpanded.value) return []
  const start = (attachmentPage.value - 1) * pageSize
  return work.value.attachments.slice(start, start + pageSize)
})

const totalAttachmentPages = computed(() => {
  return Math.ceil((work.value?.attachments?.length || 0) / pageSize)
})

function dimPercent(val: string | undefined, max: number): string {
  if (!val) return '0%'
  const num = parseFloat(val)
  if (isNaN(num) || max <= 0) return '0%'
  return Math.min(100, Math.round((num / max) * 100)) + '%'
}

function toggleAttachmentExpand() {
  attachmentExpanded.value = !attachmentExpanded.value
  if (!attachmentExpanded.value) attachmentPage.value = 1
}

function attachmentIcon(fileType: string): Component {
  const t = (fileType || '').toLowerCase()
  if (IMAGE_TYPES.includes(t)) return Picture
  if (t === 'mp4' || t === 'avi' || t === 'mov' || t === 'mkv') return VideoCamera
  if (t === 'pdf' || t === 'doc' || t === 'docx') return Document
  return Files
}

function syncLoginState() {
  isLoggedIn.value = hasLoginToken()
  currentUser.value = getCachedUserInfo()
  if (!isLoggedIn.value) {
    replyTargetId.value = null
    replyTargetName.value = ''
  }
}

function goToLogin() {
  router.push('/login')
}

function getRouteWorkId() {
  const id = route.params.id
  return typeof id === 'string' ? id : Array.isArray(id) ? id[0] : ''
}

function openPreview() {
  if (!work.value?.previewUrl) return
  previewUrl.value = work.value.previewUrl
  previewDialog.value?.open()
}

function openVideo(media: WorkAttachment) {
  activeVideoUrl.value = media.fileUrl
  activeVideoName.value = media.fileName
  videoDialogVisible.value = true
}

async function loadDetail() {
  loading.value = true
  infoExpanded.value = false
  try {
    const res = await getPublicWorkDetail(getRouteWorkId())
    const data = res.data
    work.value = data
    liked.value = data.liked ?? false
    likeCount.value = data.likeCount ?? 0
    viewCount.value = data.viewCount ?? 0
    await nextTick()
    checkInfoOverflow()
  } catch (e: unknown) {
    console.error('WorkDetail load failed:', e)
    ElMessage.error('加载失败: ' + (e instanceof Error ? e.message : '未知错误'))
  } finally {
    loading.value = false
  }
}

function checkInfoOverflow() {
  const panel = document.querySelector('.work-info-panel .work-info-content')
  if (panel) {
    isInfoOverflow.value = panel.scrollHeight > panel.clientHeight
  }
}

async function handleLike() {
  if (!isLoggedIn.value) {
    ElMessage.info('登录后可点赞')
    goToLogin()
    return
  }
  try {
    const res = await toggleLike(getRouteWorkId())
    liked.value = res.data.liked
    likeCount.value = res.data.likeCount
  } catch {
    // ignore
  }
}

async function loadComments(reset = false) {
  if (reset) commentPage.value = 1
  try {
    const res = await getCommentList(getRouteWorkId(), commentPage.value, 10)
    const data = res.data
    const list = data?.records || []
    if (reset) {
      comments.value = list
    } else {
      comments.value = [...comments.value, ...list]
    }
    commentTotal.value = data?.total || 0
  } catch {
    if (reset) comments.value = []
    commentTotal.value = 0
  }
}

async function submitComment() {
  const text = commentText.value.trim()
  if (!text) return
  commentSending.value = true
  try {
    if (isLoggedIn.value) {
      await addComment(getRouteWorkId(), text)
    } else {
      await addComment(getRouteWorkId(), text, undefined, guestName.value.trim() || undefined)
    }
    ElMessage.success('评论成功')
    commentText.value = ''
    guestName.value = ''
    loadComments(true)
  } finally {
    commentSending.value = false
  }
}

function loadMoreComments() {
  commentPage.value++
  loadComments(false)
}

function openReply(id: string | number, userName: string) {
  replyTargetId.value = id
  replyTargetName.value = userName || ''
}

function cancelReply() {
  replyTargetId.value = null
  replyTargetName.value = ''
}

async function submitReply(parentId: string | number, content: string, replyGuestName?: string) {
  const text = content.trim()
  if (!text) return
  replySending.value = true
  try {
    if (isLoggedIn.value) {
      await addComment(getRouteWorkId(), text, parentId)
    } else {
      await addComment(getRouteWorkId(), text, parentId, replyGuestName || guestName.value.trim() || undefined)
    }
    ElMessage.success('回复成功')
    cancelReply()
    loadComments(true)
  } finally {
    replySending.value = false
  }
}

async function handleDeleteComment(commentId: string | number) {
  try {
    await ElMessageBox.confirm('删除这条评论后，下面的所有回复也会一并删除，是否继续？', '删除评论', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteComment(commentId)
    ElMessage.success('评论已删除')
    if (replyTargetId.value === commentId) {
      cancelReply()
    }
    loadComments(true)
  } catch (error: unknown) {
    if (error === 'cancel' || (error instanceof Error && error.message === 'cancel')) return
  }
}

function downloadFile(url: string, name: string) {
  const a = document.createElement('a')
  a.href = url
  a.download = name
  a.click()
}

onMounted(() => {
  syncLoginState()
  window.addEventListener('focus', syncLoginState)
  window.addEventListener('storage', syncLoginState)
  loadDetail()
  loadComments(true)
})

onBeforeUnmount(() => {
  window.removeEventListener('focus', syncLoginState)
  window.removeEventListener('storage', syncLoginState)
})
</script>

<style scoped>
/* ===== 容器 ===== */
.detail-stack {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.detail-back {
  margin-top: 6px;
}

/* ===== HERO ===== */
.detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(300px, 0.8fr);
  gap: 28px;
  align-items: start;
  padding: 28px 32px;
}

.detail-hero__meta {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-hero__title {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(32px, 5vw, 54px);
  line-height: 1.08;
  letter-spacing: 0.02em;
}

.detail-hero__info {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 20px;
  color: var(--text-muted);
  font-size: 13px;
}

.info-dot {
  position: relative;
  padding-right: 20px;
}
.info-dot::after {
  content: '';
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--border-color);
}

.detail-hero__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.detail-hero__summary {
  margin: 0;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.95;
}

.detail-hero__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 4px;
  padding-top: 16px;
  border-top: 1px solid var(--border-subtle);
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 18px;
}

.hero-stat-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0;
  border: none;
  background: none;
  color: var(--text-muted);
  font-size: 14px;
  cursor: pointer;
  transition: color var(--transition-fast);
}
.hero-stat-btn:hover {
  color: #e74c3c;
}
.hero-stat-btn.is-liked {
  color: #e74c3c;
}

.hero-stat-line {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
  font-size: 14px;
}

.hero-actions-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* --- Cover --- */
.detail-hero__cover {
  position: relative;
  min-height: 380px;
  border-radius: 28px;
  overflow: hidden;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--page-bg-alt) 60%, transparent), transparent),
    radial-gradient(ellipse at 50% 0%, color-mix(in srgb, var(--brand-soft) 40%, transparent), transparent 70%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-cover {
  width: 100%;
  height: 100%;
  min-height: 350px;
  object-fit: contain;
}

.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 350px;
  color: var(--text-muted);
}

/* --- 评分浮标 --- */
.score-badge {
  position: absolute;
  right: 16px;
  bottom: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 18px 12px;
  border-radius: 20px;
  background: color-mix(in srgb, var(--card-bg-strong) 92%, transparent);
  backdrop-filter: blur(16px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
  border: 1px solid color-mix(in srgb, var(--border-color) 50%, transparent);
  line-height: 1;
  min-width: 88px;
}

.score-badge__num {
  font-family: var(--font-display);
  font-size: 40px;
  color: var(--brand);
  letter-spacing: -0.02em;
}

.score-badge__label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.score-badge__rank {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border-subtle);
  font-size: 13px;
  color: var(--text-muted);
  white-space: nowrap;
}

/* ===== 信息 Grid ===== */
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

/* ===== Info / Member 列表 ===== */
.info-list,
.member-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.info-item {
  display: flex;
  gap: 18px;
  align-items: flex-start;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-subtle);
}
.info-item:last-child {
  padding-bottom: 0;
  border-bottom: none;
}
.info-label {
  width: 86px;
  flex-shrink: 0;
  color: var(--text-muted);
}
.info-value {
  color: var(--text-primary);
}
.tag-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.text-muted {
  color: var(--text-muted);
}

.detail-block {
  margin-top: 22px;
}
.detail-block h3 {
  margin: 0 0 12px;
  font-size: 15px;
  color: var(--text-secondary);
}
.run-desc {
  margin: 0;
  padding: 18px;
  border-radius: 18px;
  background: color-mix(in srgb, var(--page-bg) 62%, transparent);
  color: var(--text-secondary);
  line-height: 1.85;
  white-space: pre-wrap;
  font-family: inherit;
}

/* ===== 作品信息面板 ===== */
.work-info-panel {
  display: flex;
  flex-direction: column;
}
.work-info-content {
  flex: 1;
  max-height: 560px;
  overflow: hidden;
  transition: max-height 0.35s ease;
  position: relative;
}
.work-info-content::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 4em;
  background: linear-gradient(transparent, var(--page-bg));
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.35s ease;
}
.work-info-panel:not(.work-info-panel--expanded) .work-info-content::after {
  opacity: 1;
}
.work-info-panel--expanded .work-info-content {
  max-height: none;
}
.work-info-panel--expanded .work-info-content::after {
  opacity: 0;
}
.info-expand-toggle {
  margin-top: 12px;
  padding: 0;
  border: none;
  background: none;
  color: var(--brand, #409eff);
  font-size: 14px;
  cursor: pointer;
  align-self: flex-start;
  transition: opacity var(--transition-fast);
}
.info-expand-toggle:hover {
  opacity: 0.75;
}

.member-item {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-subtle);
}
.member-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.member-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.member-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.member-meta {
  color: var(--text-muted);
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
  padding: 14px 16px;
  margin-bottom: 8px;
  border: 1px solid var(--border-subtle);
  border-radius: 18px;
  background: color-mix(in srgb, var(--card-bg) 84%, transparent);
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
}

.attachment-pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--border-subtle);
}

/* ===== 图片略缩图 ===== */
.image-gallery {
  margin-bottom: 22px;
}
.image-gallery__grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.gallery-thumb {
  width: 120px;
  height: 120px;
  border-radius: 14px;
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
  border-radius: 14px;
}

/* ===== 视频略缩图 ===== */
.gallery-thumb--video {
  position: relative;
  overflow: hidden;
  cursor: pointer;
}
.gallery-thumb__video-el {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.gallery-thumb__video-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.35);
  color: #fff;
  transition: background 0.25s ease;
}
.gallery-thumb--video:hover .gallery-thumb__video-overlay {
  background: rgba(0, 0, 0, 0.55);
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

/* ===== 评分详情 ===== */
.score-detail {
  padding: 24px 28px;
}
.score-detail .section-heading {
  margin-bottom: 24px;
}

.score-detail__summary {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.score-detail__big {
  font-family: var(--font-display);
  font-size: 44px;
  line-height: 0.9;
  color: var(--brand);
}
.score-detail__unit {
  font-size: 16px;
  color: var(--text-muted);
}

.score-dimensions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.score-dim {
  padding: 18px 20px;
  border: 1px solid var(--border-subtle);
  border-radius: 18px;
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
}
.score-dim__val {
  font-family: var(--font-display);
  font-size: 22px;
  line-height: 1;
  color: var(--text-primary);
}
.score-dim__bar {
  height: 6px;
  border-radius: 99px;
  background: var(--border-subtle);
  overflow: hidden;
  margin-bottom: 6px;
}
.score-dim__fill {
  height: 100%;
  border-radius: 99px;
  background: linear-gradient(90deg, var(--brand), color-mix(in srgb, var(--brand) 70%, var(--accent)));
  transition: width 0.8s cubic-bezier(0.22, 1, 0.36, 1);
}
.score-dim__fill.fill--blue {
  background: linear-gradient(90deg, #5a748d, #7fa6c9);
}
.score-dim__fill.fill--green {
  background: linear-gradient(90deg, #2f7e64, #62b096);
}
.score-dim__fill.fill--gold {
  background: linear-gradient(90deg, #b57c2f, #d1a15f);
}
.score-dim__max {
  font-size: 11px;
  color: var(--text-muted);
}

/* ===== 评论 ===== */
.comment-input,
.comment-login-tip {
  margin-bottom: 24px;
}
.comment-input--guest {
  background: var(--page-bg);
  border-radius: 16px;
  padding: 16px;
}
.guest-name-row {
  margin-bottom: 12px;
}
.guest-name-input {
  max-width: 260px;
}
.comment-submit {
  margin-top: 12px;
}
.comment-login-actions {
  margin-top: 12px;
}
.comment-list {
  display: flex;
  flex-direction: column;
}
.load-more {
  margin-top: 14px;
  text-align: center;
}

/* ===== 响应式 ===== */
@media (max-width: 1100px) {
  .score-dimensions {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1024px) {
  .detail-hero,
  .detail-grid {
    grid-template-columns: 1fr;
  }
  .detail-hero {
    gap: 24px;
    padding: 24px;
  }
  .detail-hero__cover {
    min-height: 300px;
  }
  .detail-cover {
    min-height: 280px;
  }
}

@media (max-width: 768px) {
  .score-dimensions {
    grid-template-columns: repeat(2, 1fr);
  }
  .detail-hero {
    padding: 20px;
  }
  .detail-hero__cover {
    min-height: 240px;
  }
  .detail-cover {
    min-height: 220px;
  }
  .score-badge {
    right: 12px;
    bottom: 12px;
    padding: 12px 14px 10px;
    min-width: 72px;
  }
  .score-badge__num {
    font-size: 32px;
  }
}

@media (max-width: 640px) {
  .detail-hero__cover {
    min-height: 200px;
  }
  .detail-cover {
    min-height: 180px;
  }
  .gallery-thumb {
    width: 90px;
    height: 90px;
  }
  .image-gallery__grid {
    gap: 8px;
  }
  .score-dimensions {
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }
  .score-dim {
    padding: 14px 16px;
  }
  .info-item {
    flex-direction: column;
    gap: 6px;
  }
}

@media (max-width: 480px) {
  .score-dimensions {
    grid-template-columns: 1fr;
  }
}
</style>
