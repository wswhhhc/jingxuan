<template>
  <div class="student-home">
    <section class="surface-panel student-hero reveal-up">
      <div>
        <span class="page-kicker">Student Overview</span>
        <h1 class="student-hero__title">欢迎回来，{{ authStore.userInfo?.realName || '同学' }}</h1>
        <p class="student-hero__copy">
          {{
            authStore.userInfo?.className || '当前未绑定班级信息'
          }}。从这里继续整理作品、补充材料并进入公共展示页查看最终呈现效果。
        </p>
      </div>
      <div class="student-hero__actions">
        <el-button type="primary" size="large" @click="router.push('/student/works/create')">
          <el-icon><Plus /></el-icon>
          提交新作品
        </el-button>
        <el-button size="large" @click="router.push('/works')">查看公共展廊</el-button>
      </div>
    </section>

    <section class="stats-grid reveal-up reveal-delay-1" v-loading="loading">
      <article class="stat-card" @click="router.push('/student/works')">
        <span class="stat-card__label">草稿</span>
        <strong class="stat-card__value">{{ stats.draft }}</strong>
        <p class="stat-card__meta">继续整理，暂未提交审核。</p>
      </article>
      <article class="stat-card" @click="router.push('/student/works')">
        <span class="stat-card__label">审核中</span>
        <strong class="stat-card__value">{{ stats.submitted }}</strong>
        <p class="stat-card__meta">已进入流程，等待教师或管理员处理。</p>
      </article>
      <article class="stat-card" @click="router.push('/student/works')">
        <span class="stat-card__label">已通过</span>
        <strong class="stat-card__value">{{ stats.approved }}</strong>
        <p class="stat-card__meta">可进一步发布与展示。</p>
      </article>
      <article class="stat-card stat-card--warning" @click="router.push('/student/works')">
        <span class="stat-card__label">被驳回</span>
        <strong class="stat-card__value">{{ stats.rejected }}</strong>
        <p class="stat-card__meta">查看原因并调整后再次提交。</p>
      </article>
    </section>

    <div class="student-home__grid reveal-up reveal-delay-2">
      <section class="surface-panel">
        <div class="section-heading">
          <div>
            <h2 class="section-heading__title">快速操作</h2>
            <p class="section-heading__meta">围绕提交、管理和预览三条常用路径。</p>
          </div>
        </div>
        <div class="quick-grid">
          <button class="quick-tile" type="button" @click="router.push('/student/works/create')">
            <el-icon size="24"><EditPen /></el-icon>
            <strong>提交作品</strong>
            <span>上传封面、摘要、附件与在线体验地址。</span>
          </button>
          <button class="quick-tile" type="button" @click="router.push('/student/works')">
            <el-icon size="24"><FolderOpened /></el-icon>
            <strong>我的作品</strong>
            <span>统一查看草稿、审核状态与发布结果。</span>
          </button>
          <button class="quick-tile" type="button" @click="router.push('/student/ranking')">
            <el-icon size="24"><Trophy /></el-icon>
            <strong>我的评分</strong>
            <span>回看教师评分反馈与排行表现。</span>
          </button>
        </div>
      </section>

      <section class="surface-panel">
        <div class="section-heading">
          <div>
            <h2 class="section-heading__title">创作提醒</h2>
            <p class="section-heading__meta">让作品更适合被浏览、被评审、被归档。</p>
          </div>
        </div>
        <div class="editorial-note">
          封面尽量保持统一比例，摘要建议用 2 到 4 句话说明问题、方法与结果；技术栈请优先填写读者能快速识别的关键词。
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/app/session/auth'
import { getMyWorks, type WorkItem } from '@/api/student/work'
import { Plus, EditPen, FolderOpened, Trophy } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const stats = ref({ draft: 0, submitted: 0, approved: 0, rejected: 0 })

async function loadStats() {
  loading.value = true
  try {
    const res = await getMyWorks({ page: 1, pageSize: 100 })
    const works = res.data?.records || []
    stats.value = {
      draft: works.filter((work: WorkItem) => work.status === 'draft').length,
      submitted: works.filter((work: WorkItem) => work.status === 'submitted').length,
      approved: works.filter((work: WorkItem) => work.status === 'approved').length,
      rejected: works.filter((work: WorkItem) => work.status === 'rejected').length,
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!authStore.userInfo) {
    authStore.fetchUserInfo().catch(() => undefined)
  }
  loadStats()
})
</script>

<style scoped>
.student-home {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.student-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}

.student-hero__title {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(32px, 4vw, 48px);
  line-height: 1.1;
}

.student-hero__copy {
  margin: 14px 0 0;
  max-width: 720px;
  color: var(--text-secondary);
  line-height: 1.9;
}

.student-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.stat-card {
  padding: 24px 22px;
  border: 1px solid var(--border-subtle);
  border-radius: 26px;
  background: linear-gradient(180deg, var(--card-bg), color-mix(in srgb, var(--card-bg) 76%, transparent));
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition:
    transform var(--transition-base),
    box-shadow var(--transition-base),
    border-color var(--transition-fast);
}

.stat-card:hover {
  transform: translateY(-4px);
  border-color: var(--border-color);
  box-shadow: var(--shadow-md);
}

.stat-card__label {
  display: inline-block;
  color: var(--text-muted);
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.stat-card__value {
  display: block;
  margin-top: 16px;
  font-family: var(--font-display);
  font-size: clamp(36px, 4vw, 54px);
  line-height: 1;
  color: var(--brand);
}

.stat-card__meta {
  margin: 12px 0 0;
  color: var(--text-secondary);
  line-height: 1.75;
}

.stat-card--warning .stat-card__value {
  color: var(--warning);
}

.student-home__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(300px, 0.75fr);
  gap: 22px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.quick-tile {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  min-height: 180px;
  padding: 22px;
  border: 1px solid var(--border-subtle);
  border-radius: 22px;
  background: linear-gradient(180deg, color-mix(in srgb, var(--brand-soft) 56%, transparent), transparent 72%);
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
  transition:
    transform var(--transition-base),
    border-color var(--transition-fast);
}

.quick-tile:hover {
  transform: translateY(-3px);
  border-color: var(--border-color);
}

.quick-tile strong {
  font-size: 18px;
}

.quick-tile span {
  color: var(--text-secondary);
  line-height: 1.8;
}

@media (max-width: 1024px) {
  .stats-grid,
  .quick-grid,
  .student-home__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .student-hero {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 640px) {
  .stats-grid,
  .quick-grid,
  .student-home__grid {
    grid-template-columns: 1fr;
  }
}
</style>
