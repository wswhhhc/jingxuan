<template>
  <div class="workspace-page notice-page">
    <section class="workspace-section workspace-filter-panel reveal-up">
      <div class="workspace-toolbar">
        <div class="workspace-toolbar__body">
          <h2 class="workspace-toolbar__title">公告筛选</h2>
          <p class="workspace-toolbar__desc">以统一编辑界面维护公告，并快速进入新增与预览流程。</p>
        </div>
        <div class="workspace-toolbar__actions">
          <el-input
            v-model="keyword"
            placeholder="搜索公告"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
          <el-button type="primary" @click="showEdit()">发布公告</el-button>
        </div>
      </div>
    </section>

    <section class="workspace-section reveal-up reveal-delay-1">
      <div class="workspace-toolbar workspace-toolbar--tight">
        <div class="workspace-toolbar__body">
          <h3 class="workspace-toolbar__title">公告列表</h3>
          <p class="workspace-toolbar__desc">当前公告按标题、发布人与发布时间组织为档案表。</p>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="编号" width="72">
          <template #default="{ $index }">
            {{ (page - 1) * size + $index + 1 }}
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="发布人" width="120">
          <template #default="{ row }">
            {{ row.publisherName || row.publisherId || '--' }}
          </template>
        </el-table-column>
        <el-table-column label="发送范围" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="scopeTagType(row.targetScope)">
              {{ scopeLabel(row.targetScope) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="notice-actions">
              <el-button size="small" plain @click="showDetail(row)">预览</el-button>
              <el-button size="small" type="primary" @click="showEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <PaginationBar v-model:page="page" v-model:size="size" :total="total" @change="reload" />
    </section>

    <el-dialog v-model="editVisible" :title="isEdit ? '编辑公告' : '发布公告'" width="650px">
      <el-form :model="form" label-width="60px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="公告内容" />
        </el-form-item>
        <el-form-item label="发送范围">
          <el-radio-group v-model="form.targetScope">
            <el-radio value="all">全体（学生+教师）</el-radio>
            <el-radio value="student">仅学生</el-radio>
            <el-radio value="teacher">仅教师</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="公告预览" width="650px">
      <h3 style="margin-bottom: 12px">{{ detailData?.title }}</h3>
      <div class="text-muted" style="margin-bottom: 16px">
        发布人：{{ detailData?.publisherName || detailData?.publisherId || '--' }} | {{ detailData?.publishTime }}
      </div>
      <div class="notice-content">{{ detailData?.content }}</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getNoticeList, createNotice, updateNotice, deleteNotice } from '@/api/admin/notice'
import type { NoticeItem } from '@/api/admin/notice'
import { useApiList } from '@/composables/useApiList'
import { useCrudDialog } from '@/composables/useCrudDialog'
import PaginationBar from '@/components/PaginationBar.vue'

const page = ref(1)
const size = ref(20)
const keyword = ref('')
const { loading, list, total, loadList } = useApiList<NoticeItem, Parameters<typeof getNoticeList>[0]>(getNoticeList)
const reload = () => loadList({ page: page.value, size: size.value, status: undefined })
const handleSearch = () => {
  page.value = 1
  reload()
}
const { editVisible, isEdit, editId, openCreate, openEdit, save } = useCrudDialog()
const detailVisible = ref(false)
const detailData = ref<NoticeItem | null>(null)
const form = reactive({ title: '', content: '', targetScope: 'all' })

const showDetail = (row: NoticeItem) => {
  detailData.value = row
  detailVisible.value = true
}

const showEdit = (row?: NoticeItem) => {
  if (row) {
    openEdit(row.id)
    form.title = row.title
    form.content = row.content
    form.targetScope = row.targetScope || 'all'
  } else {
    openCreate()
    form.title = ''
    form.content = ''
    form.targetScope = 'all'
  }
}

const handleSave = async () => {
  if (!form.title || !form.content) {
    ElMessage.warning('请填写完整')
    return
  }
  const ok = await save(
    () =>
      isEdit.value
        ? updateNotice(editId.value, { title: form.title, content: form.content, targetScope: form.targetScope })
        : createNotice({ title: form.title, content: form.content, status: 1, targetScope: form.targetScope }),
    { label: '公告' },
  )
  if (ok) reload()
}

function scopeLabel(scope?: string) {
  const map: Record<string, string> = { all: '全体', student: '学生', teacher: '教师' }
  return map[scope || 'all'] || '全体'
}

function scopeTagType(scope?: string): string {
  const map: Record<string, string> = { all: 'info', student: 'success', teacher: 'warning' }
  return map[scope || 'all'] || 'info'
}

const handleDelete = async (row: NoticeItem) => {
  try {
    await ElMessageBox.confirm('确认删除此公告？', '提示')
    await deleteNotice(row.id)
    ElMessage.success('已删除')
    reload()
  } catch {
    /* 取消或错误 */
  }
}

onMounted(reload)
</script>

<style scoped>
.notice-page {
  max-width: 1220px;
  margin: 0 auto;
}

.notice-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
  gap: 8px;
}

.notice-actions :deep(.el-button) {
  min-width: 0;
  margin-left: 0;
  padding-inline: 14px;
}

.notice-page :deep(.el-table) {
  --el-table-tr-bg-color: color-mix(in srgb, var(--card-bg) 92%, transparent);
  --el-fill-color-lighter: color-mix(in srgb, var(--card-bg) 88%, transparent);
}

.notice-page :deep(.el-table tr) {
  background-color: var(--el-table-tr-bg-color);
}

.notice-page :deep(.el-table__row--striped td.el-table__cell) {
  background: color-mix(in srgb, var(--brand-soft) 28%, var(--card-bg));
}

.notice-page :deep(.el-table__body tr:hover > td.el-table__cell) {
  background: color-mix(in srgb, var(--brand-soft) 44%, var(--card-bg)) !important;
}

.text-muted {
  color: var(--text-muted);
  font-size: 13px;
}
.notice-content {
  white-space: pre-wrap;
  line-height: 1.8;
}
</style>
