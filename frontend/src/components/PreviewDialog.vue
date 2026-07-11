<template>
  <el-dialog
    v-model="visible"
    title="在线体验"
    :width="isFullscreen ? '100vw' : '70vw'"
    :top="isFullscreen ? '0' : '5vh'"
    :fullscreen="isFullscreen"
    :destroy-on-close="true"
    class="preview-dialog"
    :class="{ 'preview-dialog--fullscreen': isFullscreen }"
    append-to-body
  >
    <template #header="{ close }">
      <div class="preview-dialog__header">
        <span class="preview-dialog__title">在线体验</span>
        <div class="preview-dialog__actions">
          <el-button size="small" plain @click="openInNewTab">
            <el-icon><Link /></el-icon> 新标签页
          </el-button>
          <el-button size="small" plain @click="toggleFullscreen">
            <el-icon><FullScreen /></el-icon> {{ isFullscreen ? '退出全屏' : '全屏' }}
          </el-button>
          <el-button size="small" circle @click="close">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>
    </template>

    <div class="preview-dialog__body">
      <div v-if="showLoading" class="preview-loading">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <span>正在加载项目...</span>
      </div>

      <div v-if="showTimeoutHint" class="preview-timeout-hint">
        <span>加载较慢？</span>
        <el-button link type="primary" @click="openInNewTab">去新标签页打开</el-button>
      </div>

      <iframe
        :src="normalizedUrl"
        class="preview-iframe"
        frameborder="0"
        sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
        @load="onIframeLoad"
      ></iframe>

      <div class="preview-login-hint">
        <el-icon><WarningFilled /></el-icon>
        <span>若项目有登录功能，弹窗内可能登录异常，建议使用「新标签页」打开</span>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeUnmount } from 'vue'
import { Link, FullScreen, Close, Loading, WarningFilled } from '@element-plus/icons-vue'

const props = defineProps<{
  url: string
}>()

const emit = defineEmits<{
  close: []
}>()

const visible = ref(false)
const isFullscreen = ref(false)
const showLoading = ref(true)
const showTimeoutHint = ref(false)
let loadTimer: ReturnType<typeof setTimeout> | null = null
let timeoutTimer: ReturnType<typeof setTimeout> | null = null

const normalizedUrl = computed(() => {
  const url = props.url?.trim() || ''
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  return `http://${url}`
})

function open() {
  visible.value = true
  showLoading.value = true
  showTimeoutHint.value = false
  timeoutTimer = setTimeout(() => {
    if (showLoading.value) {
      showLoading.value = false
      showTimeoutHint.value = true
    }
  }, 10000)
}

function close() {
  visible.value = false
  isFullscreen.value = false
  showLoading.value = true
  showTimeoutHint.value = false
  if (loadTimer) clearTimeout(loadTimer)
  if (timeoutTimer) clearTimeout(timeoutTimer)
  emit('close')
}

function openInNewTab() {
  if (normalizedUrl.value) {
    window.open(normalizedUrl.value, '_blank')
  }
}

function toggleFullscreen() {
  isFullscreen.value = !isFullscreen.value
}

function onIframeLoad() {
  if (loadTimer) clearTimeout(loadTimer)
  loadTimer = setTimeout(() => {
    showLoading.value = false
    showTimeoutHint.value = false
  }, 500)
  if (timeoutTimer) clearTimeout(timeoutTimer)
}

onBeforeUnmount(() => {
  if (loadTimer) clearTimeout(loadTimer)
  if (timeoutTimer) clearTimeout(timeoutTimer)
})

defineExpose({ open, close })
</script>

<!--
  全局样式说明：el-dialog 设置了 append-to-body，
  弹窗 DOM 被移出组件树，scoped 样式无法穿透。
  因此弹窗尺寸必须用全局（非 scoped）样式。
-->
<style>
.preview-dialog .el-dialog__body {
  padding: 0 !important;
  height: 80vh !important;
  overflow: hidden !important;
}
.preview-dialog--fullscreen .el-dialog__body {
  height: calc(100vh - 46px) !important;
}
.preview-dialog--fullscreen.el-dialog {
  max-width: none !important;
  margin: 0 !important;
}
.preview-dialog--fullscreen .el-dialog__header {
  padding-top: 10px !important;
  padding-bottom: 10px !important;
}
.preview-login-hint {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 6px 12px;
  background: #e6f7ff;
  border-top: 1px solid #91d5ff;
  font-size: 12px;
  color: #0050b3;
  z-index: 11;
}
</style>

<style scoped>
.preview-dialog__header {
  border-bottom: 1px solid var(--border-subtle, #e4e7ed);
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  display: flex;
}
.preview-dialog__title {
  font-size: 15px;
  font-weight: 600;
}
.preview-dialog__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.preview-dialog__body {
  position: relative;
  width: 100%;
  height: 100%;
  background: #fff;
}
.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}
.preview-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--text-secondary, #909399);
  background: var(--bg-color, #f5f7fa);
  z-index: 10;
}
.preview-loading .is-loading {
  animation: rotating 1.4s linear infinite;
}
@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
.preview-timeout-hint {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px 16px;
  background: #fffbe6;
  border-bottom: 1px solid #ffe58f;
  font-size: 13px;
  color: #ad8b00;
  z-index: 11;
}
</style>
