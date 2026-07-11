<template>
  <div class="public-layout">
    <header class="public-header">
      <div class="public-header__inner">
        <button class="public-brand" type="button" @click="router.push('/works')">
          <span class="public-brand__eyebrow">Academy Curation</span>
          <span class="public-brand__title">学院作品展示平台</span>
        </button>

        <nav class="public-nav">
          <router-link class="nav-link" to="/works">作品展廊</router-link>
          <router-link class="nav-link" to="/ranking">榜单与奖项</router-link>
        </nav>

        <div class="public-header__actions">
          <AppThemeToggle />
          <router-link :to="entryPath" class="public-entry">
            <span class="public-entry__label">{{ isLoggedIn ? '进入工作台' : '账户入口' }}</span>
            <strong>{{ isLoggedIn ? '学生端' : '登录' }}</strong>
          </router-link>
        </div>
      </div>
    </header>

    <main class="public-main">
      <router-view />
    </main>

    <footer class="public-footer">
      <div class="public-footer__inner">
        <div>
          <p class="public-footer__eyebrow">Curated Archive</p>
          <p class="public-footer__title">学院作品展示平台</p>
        </div>
        <p class="public-footer__text">
          让课程成果、实验原型与成熟作品在同一套叙事里被看见，既是公开展示，也是持续归档。
        </p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppThemeToggle from '@/components/AppThemeToggle.vue'
import { hasLoginToken } from '@/utils/auth'

const router = useRouter()
const isLoggedIn = ref(false)

const syncLoginState = () => {
  isLoggedIn.value = hasLoginToken()
}

const entryPath = computed(() => (isLoggedIn.value ? '/student/home' : '/login'))

onMounted(() => {
  syncLoginState()
  window.addEventListener('focus', syncLoginState)
  window.addEventListener('storage', syncLoginState)
})

onBeforeUnmount(() => {
  window.removeEventListener('focus', syncLoginState)
  window.removeEventListener('storage', syncLoginState)
})
</script>

<style scoped>
.public-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.public-header {
  position: sticky;
  top: 0;
  z-index: 20;
  padding: 18px 24px 0;
  backdrop-filter: blur(18px);
}

.public-header__inner {
  width: min(100%, var(--shell-max-width));
  margin: 0 auto;
  padding: 14px 18px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 20px;
  border: 1px solid var(--border-subtle);
  border-radius: 28px;
  background: color-mix(in srgb, var(--surface-elevated) 84%, transparent);
  box-shadow: var(--shadow-sm);
}

.public-brand {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 0;
  border: none;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.public-brand__eyebrow,
.public-footer__eyebrow {
  color: var(--text-muted);
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.public-brand__title,
.public-footer__title {
  font-family: var(--font-display);
  font-size: 24px;
  line-height: 1;
  color: var(--text-primary);
}

.public-nav {
  display: flex;
  justify-content: center;
  gap: 26px;
}

.public-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.public-entry {
  min-width: 132px;
  padding: 10px 16px;
  border: 1px solid var(--border-subtle);
  border-radius: 18px;
  background: linear-gradient(140deg, var(--brand-soft), transparent 72%);
  transition:
    transform var(--transition-base),
    border-color var(--transition-fast);
}

.public-entry:hover {
  transform: translateY(-1px);
  border-color: var(--border-color);
}

.public-entry__label {
  display: block;
  color: var(--text-muted);
  font-size: 11px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.public-entry strong {
  display: block;
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 15px;
}

.public-main {
  flex: 1;
  padding-top: 20px;
}

.public-footer {
  padding: 0 24px 24px;
}

.public-footer__inner {
  width: min(100%, var(--shell-max-width));
  margin: 0 auto;
  padding: 24px 28px;
  display: grid;
  grid-template-columns: auto minmax(280px, 540px);
  justify-content: space-between;
  gap: 20px;
  border-top: 1px solid var(--border-subtle);
  color: var(--text-secondary);
}

.public-footer__text {
  margin: 0;
  text-align: right;
}

@media (max-width: 960px) {
  .public-header {
    padding-inline: 16px;
  }

  .public-header__inner {
    grid-template-columns: 1fr;
    justify-items: flex-start;
  }

  .public-nav {
    justify-content: flex-start;
    flex-wrap: wrap;
    gap: 18px;
  }

  .public-header__actions {
    width: 100%;
    justify-content: space-between;
  }

  .public-footer {
    padding-inline: 16px;
  }

  .public-footer__inner {
    grid-template-columns: 1fr;
  }

  .public-footer__text {
    text-align: left;
  }
}

@media (max-width: 640px) {
  .public-brand__title,
  .public-footer__title {
    font-size: 20px;
  }

  .public-header__actions {
    flex-wrap: wrap;
  }

  .public-entry {
    width: 100%;
  }
}
</style>
