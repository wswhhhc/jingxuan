<template>
  <div class="workspace-page comment-admin-page">
    <section class="workspace-section workspace-filter-panel reveal-up">
      <div class="workspace-toolbar">
        <div class="workspace-toolbar__body">
          <h2 class="workspace-toolbar__title">评论筛选</h2>
          <p class="workspace-toolbar__desc">从作品、评论人和评论内容三个入口筛选评论治理对象。</p>
        </div>
      </div>

      <el-form :model="query" inline>
        <el-form-item label="作品">
          <el-select v-model="query.workId" clearable filterable placeholder="全部作品">
            <el-option v-for="item in workOptions" :key="item.workId" :label="item.workTitle" :value="item.workId" />
          </el-select>
        </el-form-item>
        <el-form-item label="评论人">
          <el-input v-model="query.userKeyword" placeholder="评论人姓名或账号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="评论内容">
          <el-input v-model="query.contentKeyword" placeholder="评论内容关键词" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item class="workspace-filter-actions">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="workspace-section reveal-up reveal-delay-1">
      <div class="workspace-toolbar workspace-toolbar--tight">
        <div class="workspace-toolbar__body">
          <h3 class="workspace-toolbar__title">全部作品评论</h3>
          <p class="workspace-toolbar__desc">保留评论内容、回复关系与作品关联，便于快速判断治理动作。</p>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="编号" width="72">
          <template #default="{ $index }">
            {{ getRowIndex($index) }}
          </template>
        </el-table-column>
        <el-table-column label="作品" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="work-cell">
              <span>{{ row.workTitle || `作品#${row.workId}` }}</span>
              <el-button link type="primary" @click="openWork(row.workId)">查看作品</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="评论人" width="160">
          <template #default="{ row }">
            <div>{{ row.userName || '--' }}</div>
            <div class="sub-text">{{ row.roleName || '未知角色' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="回复关系" width="180">
          <template #default="{ row }">
            <span v-if="row.replyToUserName">回复给 {{ row.replyToUserName }}</span>
            <span v-else>顶级评论</span>
          </template>
        </el-table-column>
        <el-table-column label="评论内容" min-width="320">
          <template #default="{ row }">
            <div class="content-cell" :class="{ expanded: row._expanded }" @click="toggleExpand(row)">
              {{ row.content }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评论时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <PaginationBar v-model:page="query.page" v-model:size="query.size" :total="total" @change="reload" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminCommentList, deleteAdminComment, getAdminCommentWorkOptions } from '@/api/admin/comment'
import type { AdminCommentItem, CommentWorkOption } from '@/api/admin/comment'
import { useApiList } from '@/composables/useApiList'
import PaginationBar from '@/components/PaginationBar.vue'

const workOptions = ref<CommentWorkOption[]>([])

const query = reactive({
  page: 1,
  size: 20,
  workId: undefined as number | undefined,
  userKeyword: '',
  contentKeyword: '',
})
const { loading, list, total, loadList } = useApiList<AdminCommentItem, Parameters<typeof getAdminCommentList>[0]>(
  getAdminCommentList,
)
const reload = () => loadList({ ...query })

const handleSearch = () => {
  query.page = 1
  reload()
}

const loadWorkOptions = async () => {
  try {
    const res = await getAdminCommentWorkOptions()
    workOptions.value = res.data || []
  } catch {
    workOptions.value = []
  }
}

const handleReset = () => {
  query.page = 1
  query.workId = undefined
  query.userKeyword = ''
  query.contentKeyword = ''
  reload()
}

const getRowIndex = (index: number) => (query.page - 1) * query.size + index + 1

const openWork = (workId: number) => {
  window.open(`/works/${workId}`, '_blank')
}

const toggleExpand = (row: AdminCommentItem & { _expanded?: boolean }) => {
  row._expanded = !row._expanded
}

const handleDelete = async (row: AdminCommentItem) => {
  try {
    await ElMessageBox.confirm('删除这条评论后，它下面的所有回复也会一并删除，是否继续？', '删除评论', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteAdminComment(row.id)
    ElMessage.success('评论已删除')
    loadList()
  } catch {
    // cancel or handled by interceptor
  }
}

onMounted(() => {
  loadWorkOptions()
  reload()
})
</script>

<style scoped>
.comment-admin-page {
  max-width: 1240px;
  margin: 0 auto;
}

.work-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.sub-text {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}
.content-cell {
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}
.content-cell:hover {
  color: var(--brand);
}
.content-cell.expanded {
  display: block;
  -webkit-line-clamp: unset;
}
</style>
