<template>
  <div class="workspace-page rule-page">
    <section class="workspace-section workspace-section--soft reveal-up">
      <div class="workspace-toolbar">
        <div class="workspace-toolbar__body">
          <h2 class="workspace-toolbar__title">LLM 内容审核规则</h2>
          <p class="workspace-toolbar__desc">统一维护违规类别、处理策略与系统提示词，保持审核逻辑结构化。</p>
        </div>
        <div class="workspace-toolbar__actions">
          <el-button @click="handleTest">连通性测试</el-button>
          <el-button type="primary" @click="showEdit()">新增规则</el-button>
        </div>
      </div>
    </section>

    <section class="workspace-section reveal-up reveal-delay-1">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="ruleName" label="规则名称" min-width="160" />
        <el-table-column label="违规类别" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag
              v-for="c in (row.enabledCategories || '').split(',')"
              :key="c"
              size="small"
              style="margin-right: 4px"
              >{{ c }}</el-tag
            >
          </template>
        </el-table-column>
        <el-table-column label="处理策略" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.onRejectAction === 'reject' ? 'danger' : row.onRejectAction === 'review' ? 'warning' : 'info'"
              size="small"
            >
              {{ row.onRejectAction === 'reject' ? '拒绝' : row.onRejectAction === 'review' ? '人工复核' : '警告' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <PaginationBar v-model:page="page" v-model:size="size" :total="total" @change="reload" />
    </section>

    <el-dialog v-model="editVisible" :title="isEdit ? '编辑规则' : '新增规则'" width="700px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="规则名称">
          <el-input v-model="form.ruleName" placeholder="如：内容安全检测" />
        </el-form-item>
        <el-form-item label="违规类别">
          <el-select v-model="form.enabledCategories" multiple placeholder="选择违规类别" style="width: 100%">
            <el-option label="色情" value="色情" />
            <el-option label="暴力" value="暴力" />
            <el-option label="辱骂" value="辱骂" />
            <el-option label="政治敏感" value="政治敏感" />
            <el-option label="广告引流" value="广告引流" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理策略">
          <el-radio-group v-model="form.onRejectAction">
            <el-radio value="reject">拒绝提交</el-radio>
            <el-radio value="review">人工复核</el-radio>
            <el-radio value="warn">仅警告</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核 Prompt">
          <el-input
            v-model="form.systemPrompt"
            type="textarea"
            :rows="8"
            placeholder="system prompt 模板，用于调用 DeepSeek API 做内容审核"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useCrudDialog } from '@/composables/useCrudDialog'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRuleList, createRule, updateRule, deleteRule, testConnection } from '@/api/admin/rule'
import type { RuleItem } from '@/api/admin/rule'
import { useApiList } from '@/composables/useApiList'
import PaginationBar from '@/components/PaginationBar.vue'

const page = ref(1)
const size = ref(20)
const { loading, list, total, loadList } = useApiList<RuleItem, Parameters<typeof getRuleList>[0]>(getRuleList)
const reload = () => loadList({ page: page.value, size: size.value })

const { editVisible, isEdit, editId, openCreate, openEdit, save } = useCrudDialog()
const form = reactive({
  ruleName: '',
  systemPrompt:
    '你是一个内容安全审核助手。请检测以下文本是否包含违规内容。违规类别包括：色情、暴力、辱骂、政治敏感、广告引流。返回JSON格式：{"passed": true/false, "category": "违规类别", "reason": "具体原因"}',
  enabledCategories: [] as string[],
  onRejectAction: 'reject',
})

const showEdit = (row?: RuleItem) => {
  if (row) {
    openEdit(row.id)
    form.ruleName = row.ruleName
    form.systemPrompt = row.systemPrompt
    form.enabledCategories = (row.enabledCategories || '').split(',').filter(Boolean)
    form.onRejectAction = row.onRejectAction
  } else {
    openCreate()
    form.ruleName = ''
    form.systemPrompt = ''
    form.enabledCategories = []
    form.onRejectAction = 'reject'
  }
}

const handleSave = async () => {
  if (!form.ruleName) {
    ElMessage.warning('请输入规则名称')
    return
  }
  const data = { ...form, enabledCategories: form.enabledCategories.join(',') }
  const ok = await save(() => (isEdit.value ? updateRule(editId.value, data) : createRule(data)), { label: '保存' })
  if (ok) reload()
}

const handleDelete = async (row: RuleItem) => {
  try {
    await ElMessageBox.confirm('确认删除此规则？', '提示')
    await deleteRule(row.id)
    ElMessage.success('已删除')
    reload()
  } catch {
    /* */
  }
}

const handleTest = async () => {
  try {
    await testConnection()
    ElMessage.success('DeepSeek API 连接正常')
  } catch {
    ElMessage.error('连接失败，请检查 API Key 配置')
  }
}

onMounted(reload)
</script>

<style scoped>
.rule-page {
  max-width: 1220px;
  margin: 0 auto;
}
</style>
