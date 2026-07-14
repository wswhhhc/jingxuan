<template>
  <div class="app-page public-work-list">
    <section class="surface-panel hero-grid reveal-up">
      <div>
        <span class="page-kicker">Digital Gallery</span>
        <h1 class="page-title">学院作品展廊</h1>
        <p class="page-summary">
          从课程实验、交互原型到完整项目，这里按展览而不是按后台列表的方式组织作品。每一件作品都保留作者、摘要、技术栈与展示入口，让浏览更像一次策展式阅读。
        </p>
      </div>

      <div class="hero-metrics">
        <div class="hero-metric">
          <span class="hero-metric__label">Works</span>
          <span class="hero-metric__value">{{ total || '--' }}</span>
        </div>
        <div class="hero-metric">
          <span class="hero-metric__label">Featured</span>
          <span class="hero-metric__value">{{ featuredCount }}</span>
        </div>
        <div class="hero-metric">
          <span class="hero-metric__label">Classes</span>
          <span class="hero-metric__value">{{ classes.length }}</span>
        </div>
        <div class="hero-metric">
          <span class="hero-metric__label">Curated</span>
          <span class="hero-metric__value">2026</span>
        </div>
      </div>
    </section>

    <section class="filter-panel reveal-up reveal-delay-1">
      <div class="section-heading">
        <div>
          <h2 class="section-heading__title">检索与筛选</h2>
          <p class="section-heading__meta">围绕作品名称、作者、技术栈与班级快速定位展件。</p>
        </div>
      </div>

      <el-form :model="query" size="large" class="search-form">
        <el-form-item>
          <el-input
            v-model="query.keyword"
            placeholder="搜索作品名称 / 作者"
            clearable
            @clear="search"
            @keyup.enter="search"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-select
            v-model="query.techStack"
            placeholder="技术栈筛选"
            clearable
            filterable
            @change="search"
            @clear="search"
          >
            <el-option v-for="tag in tags" :key="tag.id" :label="tag.dictLabel" :value="tag.dictLabel" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.classId" placeholder="班级筛选" clearable @change="search">
            <el-option v-for="c in classes" :key="c.id" :label="c.dictLabel" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-form__range">
          <el-date-picker
            v-model="submitTimeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="提交起始"
            end-placeholder="提交截止"
            value-format="YYYY-MM-DD HH:mm:ss"
            @change="onDateRangeChange"
          />
        </el-form-item>
        <el-form-item class="search-form__actions">
          <el-button type="primary" @click="search">搜索作品</el-button>
          <el-button @click="resetSearch">重置条件</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="editorial-note reveal-up reveal-delay-2">
      精选作品会在卡片右上角标出，详情页中可继续查看封面、成员、附件、评论与在线体验入口。
    </section>

    <section v-loading="loading" class="reveal-up reveal-delay-3">
      <el-empty v-if="!loading && list.length === 0" description="暂无展示作品" />

      <div v-else class="work-grid">
        <article v-for="item in list" :key="item.id" class="work-card" @click="router.push(`/works/${item.id}`)">
          <div class="work-card__cover">
            <el-image :src="item.coverUrl || '/placeholder-cover.png'" fit="contain" class="cover-img">
              <template #error>
                <div class="cover-placeholder">
                  <el-icon :size="36"><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <el-tag v-if="item.featured" class="work-card__badge" effect="dark" type="warning">精选</el-tag>
          </div>

          <div class="work-card__body">
            <div class="work-card__topline">
              <span>{{ item.submitTime?.split(' ')[0] || '未标注日期' }}</span>
              <span>{{ item.submitterName || '团队作品' }}</span>
            </div>
            <h3 class="work-card__title">{{ item.title }}</h3>
            <p class="work-card__summary">
              {{
                item.summary
                  ? item.summary.length > 100
                    ? item.summary.slice(0, 100) + '...'
                    : item.summary
                  : '暂无作品简介。'
              }}
            </p>
            <div class="work-card__tags" v-if="item.tags?.length">
              <el-tag v-for="tag in item.tags.slice(0, 4)" :key="tag" size="small" effect="plain">
                {{ tag }}
              </el-tag>
            </div>
            <div class="work-card__footer">
              <div class="work-card__stats">
                <span class="work-card__stat">
                  <el-icon :size="14"><View /></el-icon>
                  {{ item.viewCount ?? 0 }}
                </span>
                <span class="work-card__stat">
                  <el-icon :size="14"><Star /></el-icon>
                  {{ item.likeCount ?? 0 }}
                </span>
              </div>
              <span class="work-card__arrow">↗</span>
            </div>
          </div>
        </article>
      </div>

      <PaginationBar v-model:page="page" v-model:size="pageSize" :total="total" @change="reload" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Picture, View, Star } from '@element-plus/icons-vue'
import {
  getPublicWorkList,
  getPublicClassList as fetchClasses,
  getPublicTagList as fetchTags,
  type PublicWorkListParams,
} from '@/api/public/work'
import type { WorkItem } from '@/api/student/work'

type V1ReferenceItem = { id: string; dictValue: string; dictLabel: string }
type V1Tag = { id: string; dictLabel: string }
import type { PageResult } from '@/api/workAdapter'
import { useApiList } from '@/composables/useApiList'
import PaginationBar from '@/components/PaginationBar.vue'

const router = useRouter()
const page = ref(1)
const pageSize = ref(12)
const classes = ref<V1ReferenceItem[]>([])
const tags = ref<V1Tag[]>([])
const submitTimeRange = ref<[string, string] | null>(null)
const { loading, list, total, loadList } = useApiList<WorkItem, PublicWorkListParams, PageResult<WorkItem>>(
  getPublicWorkList,
  (data) => ({
    records: data?.records || [],
    total: data?.total || 0,
  }),
)
const reload = () =>
  loadList({
    page: page.value,
    pageSize: pageSize.value,
    keyword: query.keyword || undefined,
    techStack: query.techStack || undefined,
    classId: query.classId || undefined,
    submitTimeBegin: query.submitTimeBegin || undefined,
    submitTimeEnd: query.submitTimeEnd || undefined,
  })

const query = reactive<PublicWorkListParams>({
  keyword: '',
  techStack: '',
})

const featuredCount = computed(() => list.value.filter((item) => Boolean(item.featured)).length)

function search() {
  page.value = 1
  reload()
}

function resetSearch() {
  query.keyword = ''
  query.techStack = ''
  query.classId = undefined
  submitTimeRange.value = null
  query.submitTimeBegin = undefined
  query.submitTimeEnd = undefined
  search()
}

function onDateRangeChange(val: [string, string] | null) {
  if (val) {
    query.submitTimeBegin = val[0]
    query.submitTimeEnd = val[1]
  } else {
    query.submitTimeBegin = undefined
    query.submitTimeEnd = undefined
  }
  search()
}

async function loadClasses() {
  try {
    const res = await fetchClasses()
    classes.value = res || []
  } catch {
    classes.value = []
  }
}

async function loadTags() {
  try {
    const res = await fetchTags()
    tags.value = res || []
  } catch {
    tags.value = []
  }
}

onMounted(() => {
  loadClasses()
  loadTags()
  reload()
})
</script>

<style scoped>
.public-work-list {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.search-form {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.search-form__range {
  grid-column: span 2;
}

.search-form__actions {
  grid-column: 1 / -1;
}

.work-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 22px;
}

.work-card {
  display: flex;
  flex-direction: column;
  min-height: 420px;
  border: 1px solid var(--border-subtle);
  border-radius: 28px;
  overflow: hidden;
  background: linear-gradient(180deg, var(--card-bg), color-mix(in srgb, var(--card-bg) 76%, transparent));
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition:
    transform var(--transition-base),
    box-shadow var(--transition-base),
    border-color var(--transition-fast);
}

.work-card:hover {
  transform: translateY(-6px);
  border-color: var(--border-color);
  box-shadow: var(--shadow-md);
}

.work-card__cover {
  position: relative;
  aspect-ratio: 1.18 / 1;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 14px;
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--brand-soft) 70%, transparent), transparent 42%),
    color-mix(in srgb, var(--page-bg-alt) 86%, transparent);
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform var(--transition-slow);
}

.work-card:hover .cover-img {
  transform: scale(1.02);
}

.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-muted);
}

.work-card__badge {
  position: absolute;
  top: 16px;
  right: 16px;
}

.work-card__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 14px;
  padding: 22px;
}

.work-card__topline,
.work-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--text-muted);
  font-size: 12px;
  letter-spacing: 0.04em;
}

.work-card__title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 24px;
  line-height: 1.2;
}

.work-card__summary {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.85;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
}

.work-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.work-card__footer {
  margin-top: auto;
  padding-top: 6px;
  border-top: 1px solid var(--border-subtle);
}

.work-card__stats {
  display: flex;
  align-items: center;
  gap: 14px;
}

.work-card__stat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.work-card__arrow {
  font-size: 18px;
  color: var(--brand);
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding-top: 10px;
}

@media (max-width: 1024px) {
  .search-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .search-form__range {
    grid-column: 1 / -1;
  }
}

@media (max-width: 640px) {
  .search-form {
    grid-template-columns: 1fr;
  }

  .search-form__range,
  .search-form__actions {
    grid-column: auto;
  }

  .work-card {
    min-height: 380px;
  }

  .work-card__body {
    padding: 18px;
  }
}
</style>






