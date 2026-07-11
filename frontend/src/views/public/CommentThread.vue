<template>
  <div class="comment-thread">
    <div class="comment-item" :class="{ 'is-nested': currentDepth > 0 }">
      <div class="comment-avatar">
        <el-avatar :size="currentDepth === 0 ? 36 : 28" :src="comment.avatarUrl || undefined">
          {{ displayName.charAt(0) }}
        </el-avatar>
      </div>
      <div class="comment-body">
        <div class="comment-header">
          <span class="comment-user">{{ displayName }}</span>
          <el-tag v-if="comment.roleName" size="small" type="info" effect="plain" class="role-tag">
            {{ comment.roleName }}
          </el-tag>
          <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
        </div>
        <div class="comment-content">
          <span v-if="comment.replyToUserName" class="reply-to">回复给 {{ comment.replyToUserName }}：</span>
          <span>{{ comment.content }}</span>
        </div>
        <div class="comment-actions">
          <el-button link size="small" @click="emit('openReply', comment.id, displayName)">回复</el-button>
          <el-button v-if="showDelete" link size="small" type="danger" @click="emit('deleteComment', comment.id)">
            删除
          </el-button>
        </div>
        <!-- 回复输入框 -->
        <div v-if="replyTargetId === comment.id" class="reply-input-box">
          <div v-if="!isLoggedIn" class="reply-guest-name">
            <el-input v-model="localGuestName" placeholder="你的昵称（选填）" maxlength="20" size="small" clearable />
          </div>
          <el-input
            v-model="localReplyText"
            type="textarea"
            :rows="2"
            :placeholder="replyPlaceholder"
            maxlength="500"
            show-word-limit
          />
          <div class="reply-actions">
            <el-button size="small" @click="emit('cancelReply')">取消</el-button>
            <el-button size="small" type="primary" :loading="replySending" @click="handleSubmitReply">回复</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 递归子回复 -->
    <div v-if="comment.replies?.length" class="reply-toggle">
      <el-button link type="primary" size="small" @click="toggleReplies">
        {{ repliesExpanded ? '收起回复' : `展开 ${comment.replies.length} 条回复` }}
      </el-button>
    </div>
    <div v-if="comment.replies?.length && repliesExpanded" class="replies-container">
      <CommentThread
        v-for="reply in comment.replies"
        :key="reply.id"
        :comment="reply"
        :depth="currentDepth + 1"
        :is-logged-in="isLoggedIn"
        :reply-target-id="replyTargetId"
        :reply-target-name="replyTargetName"
        :reply-sending="replySending"
        :current-user-id="currentUserId"
        :is-admin="isAdmin"
        :is-work-leader="isWorkLeader"
        @open-reply="(id, userName) => emit('openReply', id, userName)"
        @cancel-reply="emit('cancelReply')"
        @submit-reply="forwardSubmitReply"
        @delete-comment="emit('deleteComment', $event)"
        @go-to-login="emit('goToLogin')"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { CommentItem } from '@/api/public/comment'

defineOptions({ name: 'CommentThread' })

const props = defineProps<{
  comment: CommentItem
  depth?: number
  isLoggedIn: boolean
  replyTargetId: string | number | null
  replyTargetName: string
  replySending: boolean
  currentUserId: string | number | null
  isAdmin: boolean
  isWorkLeader: boolean
}>()

const emit = defineEmits<{
  (e: 'openReply', id: string | number, userName: string): void
  (e: 'cancelReply'): void
  (e: 'submitReply', parentId: string | number, content: string, guestName?: string): void
  (e: 'deleteComment', id: string | number): void
  (e: 'goToLogin'): void
}>()

const localReplyText = ref('')
const localGuestName = ref('')
const repliesExpanded = ref(false)

const displayName = computed(() => props.comment.userName || '匿名用户')
const currentDepth = computed(() => props.depth ?? 0)
const showDelete = computed(() => {
  const commentUserId = props.comment.userId
  // 游客评论（userId 为 null/undefined）仅管理员可删
  if (commentUserId === null || commentUserId === undefined) {
    return props.isLoggedIn && props.isAdmin
  }
  if (!props.isLoggedIn || props.currentUserId == null) return false
  return commentUserId === props.currentUserId || props.isAdmin || props.isWorkLeader
})
const replyPlaceholder = computed(() => {
  return props.replyTargetName ? `回复给 ${props.replyTargetName}...` : '回复评论...'
})

watch(
  () => props.replyTargetId,
  () => {
    localReplyText.value = ''
    localGuestName.value = ''
  },
)

function forwardSubmitReply(parentId: string | number, content: string, guestName?: string) {
  emit('submitReply', parentId, content, guestName)
}

function handleSubmitReply() {
  const text = localReplyText.value.trim()
  if (!text) return
  emit('submitReply', props.comment.id, text, localGuestName.value.trim() || undefined)
  localReplyText.value = ''
  localGuestName.value = ''
}

function toggleReplies() {
  repliesExpanded.value = !repliesExpanded.value
}

function formatTime(iso: string): string {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<style scoped>
.comment-thread {
  width: 100%;
}
.comment-item {
  display: flex;
  gap: 12px;
  padding: 18px 0;
  border-bottom: 1px solid var(--border-subtle);
}
.comment-item.is-nested {
  padding: 10px 0;
  border-bottom: none;
}
.comment-item.is-nested + .comment-item.is-nested {
  border-top: 1px dashed var(--border-subtle);
}
.comment-avatar {
  flex-shrink: 0;
  padding-top: 2px;
}
.comment-body {
  flex: 1;
  min-width: 0;
}
.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}
.comment-user {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
.role-tag {
  font-size: 11px;
}
.comment-time {
  font-size: 12px;
  color: var(--text-muted);
}
.comment-content {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.85;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: break-word;
}
.reply-to {
  color: var(--brand);
  margin-right: 4px;
}
.comment-actions {
  margin-top: 8px;
}
.reply-input-box {
  margin-top: 12px;
}
.reply-guest-name {
  margin-bottom: 8px;
  max-width: 240px;
}
.reply-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}
.reply-toggle {
  margin-top: 4px;
  padding-left: 48px;
}
.replies-container {
  margin-top: 8px;
  padding-left: 24px;
  border-left: 1px solid var(--border-subtle);
}
</style>
