<template>
  <div class="workspace-page dict-page">
    <section class="workspace-section reveal-up">
      <div class="workspace-toolbar">
        <div class="toolbar-left">
          <el-select
            v-model="currentType"
            placeholder="选择字典类型"
            class="dict-type-select"
            :style="{ width: `${selectWidth}px` }"
            @change="onTypeChange"
          >
            <el-option v-for="t in typeList" :key="t" :label="t" :value="t" />
          </el-select>
          <span v-if="currentType" class="type-label">{{ currentTypeLabel }}</span>
        </div>
        <div class="workspace-toolbar__actions">
          <el-button type="primary" :disabled="!currentType" @click="showEdit()">新增字典项</el-button>
        </div>
      </div>

      <el-table :data="pagedItems" v-loading="loading" stripe v-if="currentType">
        <el-table-column label="编号" width="60">
          <template #default="{ $index }">
            {{ getRowIndex($index) }}
          </template>
        </el-table-column>
        <el-table-column prop="dictLabel" label="标签" width="160" />
        <el-table-column prop="dictValue" label="值" width="160" />
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="showEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-else description="请选择字典类型" />

      <div class="workspace-pagination" v-if="currentType">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @change="loadItems"
        />
      </div>
    </section>

    <el-dialog v-model="editVisible" :title="isEdit ? '编辑字典项' : '新增字典项'" width="550px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="字典类型">
          <el-input v-model="form.dictType" :disabled="isEdit" placeholder="如：work_status" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.dictLabel" placeholder="如：已通过" />
        </el-form-item>
        <el-form-item label="值">
          <el-input v-model="form.dictValue" placeholder="如：3" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="备注说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllDicts, createDict, updateDict, deleteDict } from '@/api/admin/dict'
import type { DictItem } from '@/api/admin/dict'
import { useCrudDialog } from '@/composables/useCrudDialog'

const loading = ref(false)
const typeMap = ref<Record<string, DictItem[]>>({})
const currentType = ref('')
const items = ref<DictItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(9999)
const { editVisible, isEdit, editId, openCreate, openEdit, save } = useCrudDialog()
const form = reactive({
  dictType: '',
  dictLabel: '',
  dictValue: '',
  sort: 0,
  remark: '',
})

const typeList = computed(() => Object.keys(typeMap.value).sort())
const selectWidth = computed(() => {
  const longestLength = Math.max(
    '选择字典类型'.length,
    ...typeList.value.map((item) => item.length),
    currentType.value.length,
  )
  return Math.min(520, Math.max(220, longestLength * 16 + 72))
})

const currentTypeLabel = computed(() => {
  const arr = typeMap.value[currentType.value]
  return arr && arr.length > 0 ? `共 ${total.value} 项` : ''
})

const pagedItems = computed(() => {
  const start = (page.value - 1) * size.value
  return items.value.slice(start, start + size.value)
})

const loadAll = async () => {
  loading.value = true
  try {
    const res = await getAllDicts()
    typeMap.value = res.data || {}
    if (currentType.value && !typeMap.value[currentType.value]) {
      currentType.value = ''
    }
    if (currentType.value) {
      loadItems()
    }
  } finally {
    loading.value = false
  }
}

const loadItems = () => {
  const all = typeMap.value[currentType.value] || []
  items.value = all
  total.value = all.length
}

const getRowIndex = (index: number) => (page.value - 1) * size.value + index + 1

const onTypeChange = () => {
  page.value = 1
  loadItems()
}

const showEdit = (row?: DictItem) => {
  if (row) {
    openEdit(row.id)
    form.dictType = row.dictType
    form.dictLabel = row.dictLabel
    form.dictValue = row.dictValue
    form.sort = row.sort ?? 0
    form.remark = row.remark || ''
  } else {
    openCreate()
    form.dictType = currentType.value
    form.dictLabel = ''
    form.dictValue = ''
    form.sort = 0
    form.remark = ''
  }
}

const handleSave = async () => {
  if (!form.dictType || !form.dictLabel || !form.dictValue) {
    ElMessage.warning('请填写完整（类型、标签、值为必填）')
    return
  }
  const ok = await save(() => (isEdit.value ? updateDict({ id: editId.value, ...form }) : createDict({ ...form })), {
    label: '字典',
  })
  if (ok) loadAll()
}

const handleDelete = async (row: DictItem) => {
  try {
    await ElMessageBox.confirm('确认删除此字典项？', '提示')
    await deleteDict(row.id)
    ElMessage.success('已删除')
    loadAll()
  } catch {
    /* 取消或错误 */
  }
}

onMounted(loadAll)
</script>

<style scoped>
.dict-page {
  max-width: 1220px;
  margin: 0 auto;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dict-type-select {
  flex: 0 0 auto;
}

.type-label {
  color: var(--text-muted);
  font-size: 13px;
}

@media (max-width: 768px) {
  .toolbar-left {
    width: 100%;
    flex-wrap: wrap;
  }

  .dict-type-select {
    width: 100% !important;
  }
}
</style>
