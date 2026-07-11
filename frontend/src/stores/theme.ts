import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'

export type ThemeMode = 'light' | 'dark'

const STORAGE_KEY = 'jingxuan-theme'

function getSystemTheme(): ThemeMode {
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function applyTheme(mode: ThemeMode) {
  document.documentElement.dataset.theme = mode
  document.documentElement.style.colorScheme = mode
}

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>('light')
  const ready = ref(false)

  const isDark = computed(() => mode.value === 'dark')

  function setMode(next: ThemeMode) {
    mode.value = next
    applyTheme(next)
    localStorage.setItem(STORAGE_KEY, next)
  }

  function toggleMode() {
    setMode(mode.value === 'light' ? 'dark' : 'light')
  }

  function initTheme() {
    if (ready.value) {
      applyTheme(mode.value)
      return
    }
    const saved = localStorage.getItem(STORAGE_KEY)
    const resolved = saved === 'light' || saved === 'dark' ? saved : getSystemTheme()
    mode.value = resolved
    applyTheme(resolved)
    ready.value = true
  }

  watch(mode, (value) => {
    if (!ready.value) return
    applyTheme(value)
  })

  return {
    mode,
    isDark,
    initTheme,
    setMode,
    toggleMode,
  }
})
