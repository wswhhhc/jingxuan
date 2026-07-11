<template>
  <button
    type="button"
    class="theme-toggle"
    :aria-label="buttonLabel"
    :title="buttonLabel"
    @click="themeStore.toggleMode()"
  >
    <el-icon :size="18">
      <MoonNight v-if="themeStore.isDark" />
      <Sunny v-else />
    </el-icon>
    <span class="theme-toggle__text">
      <span class="theme-toggle__label">Theme</span>
      <span class="theme-toggle__mode">{{ modeLabel }}</span>
    </span>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { MoonNight, Sunny } from '@element-plus/icons-vue'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()

const modeLabel = computed(() => (themeStore.isDark ? '夜展模式' : '纸面模式'))
const buttonLabel = computed(() => (themeStore.isDark ? '切换到浅色纸面模式' : '切换到深色夜展模式'))
</script>
