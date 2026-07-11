import { ref } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * CRUD 弹窗状态管理 composable
 *
 * 统一管理 editVisible / isEdit / editId 状态，
 * 以及 openCreate / openEdit / save 方法。
 *
 * @example
 *   const { editVisible, isEdit, openCreate, openEdit, save } = useCrudDialog()
 *
 *   // 打开创建弹窗
 *   openCreate()
 *
 *   // 打开编辑弹窗
 *   openEdit(row.id)
 *
 *   // 保存（自动 try-catch + ElMessage）
 *   const handleSave = async () => {
 *     if (!validate()) return
 *     await save(
 *       () => isEdit.value ? updateApi(editId.value, payload) : createApi(payload),
 *       { label: '保存' }
 *     )
 *     reload()
 *   }
 */
export function useCrudDialog() {
  const editVisible = ref(false)
  const isEdit = ref(false)
  const editId = ref(0)

  function openCreate() {
    isEdit.value = false
    editId.value = 0
    editVisible.value = true
  }

  function openEdit(id: number) {
    isEdit.value = true
    editId.value = id
    editVisible.value = true
  }

  async function save(apiCall: () => Promise<unknown>, options?: { label?: string }): Promise<boolean> {
    const label = options?.label || '操作'
    try {
      await apiCall()
      ElMessage.success(isEdit.value ? '已更新' : '已创建')
      editVisible.value = false
      return true
    } catch (e) {
      console.error(`${label}失败:`, e)
      ElMessage.error(`${label}失败，请重试`)
      return false
    }
  }

  return { editVisible, isEdit, editId, openCreate, openEdit, save }
}
