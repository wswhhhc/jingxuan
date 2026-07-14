<template>
  <div class="workspace-page user-page">
    <section class="workspace-section workspace-filter-panel reveal-up">
      <div class="workspace-toolbar">
        <div class="workspace-toolbar__body">
          <h2 class="workspace-toolbar__title">用户筛选</h2>
          <p class="workspace-toolbar__desc">按关键词、角色和状态切换列表，保持账号档案的清晰度。</p>
        </div>
      </div>

      <el-form :model="query" inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="用户名/姓名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.roleId" clearable placeholder="全部">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
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
          <h3 class="workspace-toolbar__title">用户列表</h3>
          <p class="workspace-toolbar__desc">集中维护账号、角色、班级与状态。</p>
        </div>
        <div class="workspace-toolbar__actions">
          <el-button @click="showBatchImport">批量导入</el-button>
          <el-button type="primary" @click="openCreate">新增用户</el-button>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="roleName" label="角色" width="100" />
        <el-table-column prop="className" label="班级" width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="user-actions">
              <el-button size="small" :disabled="isProtectedAdmin(row)" @click="openEdit(row)">编辑</el-button>
              <el-button
                size="small"
                :type="row.status === 1 ? 'warning' : 'success'"
                :disabled="isProtectedAdmin(row)"
                @click="handleToggleStatus(row)"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button size="small" plain :disabled="isProtectedAdmin(row)" @click="handleResetPwd(row)"
                >重置密码</el-button
              >
              <el-button size="small" type="danger" plain :disabled="isProtectedAdmin(row)" @click="handleDelete(row)"
                >删除</el-button
              >
            </div>
          </template>
        </el-table-column>
      </el-table>

      <PaginationBar v-model:page="query.page" v-model:size="query.size" :total="total" @change="reload" />
    </section>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px" size="large">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="form.classId" placeholder="请选择班级" clearable style="width: 100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="400px" destroy-on-close>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="pwdForm.password" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPwd">
          <el-input v-model="pwdForm.confirmPwd" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSavePwd">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchVisible" title="批量导入用户" width="880px" destroy-on-close>
      <el-tabs v-model="batchTab" class="batch-import-tabs">
        <el-tab-pane label="AI 辅助导入" name="ai">
          <div class="ai-import-panel">
            <div class="ai-import-chat">
              <div class="ai-import-meta">
                <span>必填：{{ aiRequiredFields.join('、') }}</span>
                <span>选填：{{ aiOptionalFields.join('、') }}</span>
              </div>
              <div class="ai-import-messages">
                <div
                  v-for="(message, index) in aiMessages"
                  :key="`${message.role}-${index}`"
                  :class="['ai-import-message', `ai-import-message--${message.role}`]"
                >
                  <span class="ai-import-message__role">{{ message.role === 'assistant' ? 'AI 助手' : '你' }}</span>
                  <p>{{ message.content }}</p>
                </div>
              </div>
              <el-input
                v-model="aiInput"
                type="textarea"
                :rows="4"
                placeholder="例如：帮我创建 5 个学生账号，用户名 user1 到 user5，密码统一 123456，班级是软件工程1班。真实姓名按 张一 到 张五。"
              />
              <div class="ai-import-actions">
                <el-button @click="resetAiImportState">重新开始</el-button>
                <el-button type="primary" :loading="aiLoading" @click="handleAiGenerate">发送给 AI</el-button>
              </div>
            </div>

            <div class="ai-import-preview">
              <div class="ai-import-hint">
                <p>AI 会先判断信息是否足够，缺必填项就继续追问；足够时会直接生成可导入预览。</p>
              </div>

              <div v-if="aiMissingFields.length" class="ai-import-summary">
                <h4>待补充信息</h4>
                <ul>
                  <li v-for="item in aiMissingFields" :key="item">{{ item }}</li>
                </ul>
              </div>

              <div v-if="aiAssumptions.length" class="ai-import-summary">
                <h4>AI 默认处理</h4>
                <ul>
                  <li v-for="item in aiAssumptions" :key="item">{{ item }}</li>
                </ul>
              </div>

              <div class="ai-import-preview__header">
                <h4>账号预览</h4>
                <el-tag :type="aiReady ? 'success' : 'info'" size="small">
                  {{ aiReady ? '可导入' : '待补充信息' }}
                </el-tag>
              </div>

              <el-table
                v-if="aiPreviewUsers.length"
                :data="aiPreviewUsers"
                size="small"
                stripe
                max-height="280"
                style="width: 100%"
              >
                <el-table-column prop="username" label="用户名" min-width="120" />
                <el-table-column prop="realName" label="真实姓名" min-width="120" />
                <el-table-column prop="roleName" label="角色" width="90" />
                <el-table-column prop="className" label="班级" min-width="140" show-overflow-tooltip />
                <el-table-column prop="password" label="密码" width="100" />
                <el-table-column label="状态" width="80">
                  <template #default="{ row }">{{ row.status === 0 ? '禁用' : '启用' }}</template>
                </el-table-column>
              </el-table>
              <el-empty v-else description="AI 生成的账号预览会显示在这里" />
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="JSON 导入" name="json">
          <p style="color: #909399; margin-bottom: 12px">请输入 JSON 数组，每个元素为一个用户对象：</p>
          <el-input
            v-model="batchJson"
            type="textarea"
            :rows="12"
            placeholder='[{"username":"test1","realName":"测试1","roleId":1,"classId":1,"phone":"13800000000","email":"t1@test.com"}]'
          />
        </el-tab-pane>
      </el-tabs>

      <div v-if="batchResult" class="batch-result">
        <p>
          成功：<strong>{{ batchResult.success }}</strong> 条，失败：<strong>{{ batchResult.failed }}</strong> 条
        </p>
        <ul v-if="batchResult.errors?.length">
          <li v-for="(e, i) in batchResult.errors" :key="i" style="color: #f56c6c">{{ e }}</li>
        </ul>
      </div>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button
          v-if="batchTab === 'ai'"
          type="primary"
          :loading="batchSaving"
          :disabled="!aiReady || !aiPreviewUsers.length"
          @click="handleAiBatchImport"
        >
          导入 AI 预览结果
        </el-button>
        <el-button v-else type="primary" :loading="batchSaving" @click="handleBatchImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  getUsers,
  createUser,
  updateUser,
  updateStatus,
  deleteUser,
  getRoles,
  getClasses,
  batchImportUsers,
  parseAiImportUsers,
} from '@/api/admin/user'
import type { UserItem, RoleItem, ClassItem, AiImportMessage, AiImportUserDraft } from '@/api/admin/user'
import { useApiList } from '@/composables/useApiList'
import PaginationBar from '@/components/PaginationBar.vue'

const { loading, list, total, loadList } = useApiList<UserItem, Parameters<typeof getUsers>[0]>(getUsers)
const reload = () => loadList({ ...query })
const saving = ref(false)
const roles = ref<RoleItem[]>([])
const classes = ref<ClassItem[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<string | null>(null)
const pwdDialogVisible = ref(false)
const resetPwdUserId = ref<string | null>(null)
const resetPwdUser = ref<UserItem | null>(null)
const formRef = ref<FormInstance>()
const pwdFormRef = ref<FormInstance>()

const query = reactive({
  page: 1,
  size: 20,
  keyword: '',
  roleId: undefined as number | undefined,
  status: undefined as number | undefined,
})

const form = reactive({
  username: '',
  realName: '',
  roleId: undefined as number | undefined,
  classId: undefined as number | undefined,
  phone: '',
  email: '',
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

const pwdForm = reactive({
  password: '',
  confirmPwd: '',
})

const pwdRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPwd: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== pwdForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

const isProtectedAdmin = (row: UserItem) => row.username === 'admin' && row.roleName === '管理员'

const handleSearch = () => {
  query.page = 1
  reload()
}

const handleReset = () => {
  query.page = 1
  query.keyword = ''
  query.roleId = undefined
  query.status = undefined
  reload()
}

const openCreate = () => {
  isEdit.value = false
  editId.value = null
  form.username = ''
  form.realName = ''
  form.roleId = undefined
  form.classId = undefined
  form.phone = ''
  form.email = ''
  dialogVisible.value = true
}

const openEdit = async (row: UserItem) => {
  if (isProtectedAdmin(row)) {
    ElMessage.warning('系统管理员不允许修改')
    return
  }
  isEdit.value = true
  editId.value = row.id
  form.username = row.username
  form.realName = row.realName
  form.roleId = row.roleId
  form.classId = row.classId
  form.phone = row.phone || ''
  form.email = row.email || ''
  dialogVisible.value = true
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value && editId.value) {
      await updateUser(editId.value, { ...form })
      ElMessage.success('编辑成功')
    } else {
      await createUser({ ...form, roleId: form.roleId! })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    reload()
  } catch {
    // handled by interceptor
  } finally {
    saving.value = false
  }
}

const handleToggleStatus = (row: UserItem) => {
  if (isProtectedAdmin(row)) {
    ElMessage.warning('系统管理员不允许修改')
    return
  }
  const action = row.status === 1 ? '禁用' : '启用'
  ElMessageBox.confirm(`确认${action}用户「${row.realName}」？`, '提示')
    .then(async () => {
      try {
        await updateStatus(row.id, row.status === 1 ? 0 : 1)
        ElMessage.success(`${action}成功`)
        reload()
      } catch {
        // handled by interceptor
      }
    })
    .catch(() => {})
}

const handleResetPwd = (row: UserItem) => {
  if (isProtectedAdmin(row)) {
    ElMessage.warning('系统管理员不允许修改')
    return
  }
  resetPwdUserId.value = row.id
  resetPwdUser.value = row
  pwdForm.password = ''
  pwdForm.confirmPwd = ''
  pwdDialogVisible.value = true
}

const handleDelete = (row: UserItem) => {
  if (isProtectedAdmin(row)) return
  ElMessageBox.confirm(`确认删除用户「${row.realName}」？`, '提示', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
    .then(async () => {
      try {
        await deleteUser(row.id)
        ElMessage.success('删除成功')
        reload()
      } catch {
        /* handled by interceptor */
      }
    })
    .catch(() => {})
}

const handleSavePwd = async () => {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const user = resetPwdUser.value
    if (!user) return
    await updateUser(resetPwdUserId.value!, {
      username: user.username,
      realName: user.realName,
      roleId: user.roleId,
      classId: user.classId,
      phone: user.phone,
      email: user.email,
      password: pwdForm.password,
    })
    ElMessage.success('密码重置成功')
    pwdDialogVisible.value = false
  } catch {
    // handled by interceptor
  } finally {
    saving.value = false
  }
}

const batchVisible = ref(false)
const batchSaving = ref(false)
const batchTab = ref<'ai' | 'json'>('ai')
const batchJson = ref('')
const batchResult = ref<{ success: number; failed: number; errors: string[] } | null>(null)
const aiLoading = ref(false)
const aiInput = ref('')
const aiMessages = ref<AiImportMessage[]>([])
const aiPreviewUsers = ref<AiImportUserDraft[]>([])
const aiReady = ref(false)
const aiRequiredFields = ref(['username', 'realName', 'role'])
const aiOptionalFields = ref(['class', 'password', 'phone', 'email', 'status'])
const aiMissingFields = ref<string[]>([])
const aiAssumptions = ref<string[]>([])

const buildAiWelcomeMessage = () => {
  const roleText = roles.value.length ? roles.value.map((item) => item.roleName).join('、') : '学生、教师、管理员'
  const classText = classes.value.length
    ? classes.value
        .slice(0, 6)
        .map((item) => item.className)
        .join('、')
    : '当前班级数据加载后会自动识别'
  return `请直接描述你想批量创建的账号。我会先判断必填项是否齐全，再帮你生成导入预览。必填是用户名、真实姓名、角色；选填是班级、密码、手机号、邮箱、状态。当前角色有：${roleText}。可识别班级示例：${classText}。`
}

const resetAiImportState = () => {
  aiInput.value = ''
  aiReady.value = false
  aiPreviewUsers.value = []
  aiMissingFields.value = []
  aiAssumptions.value = []
  aiRequiredFields.value = ['username', 'realName', 'role']
  aiOptionalFields.value = ['class', 'password', 'phone', 'email', 'status']
  aiMessages.value = [{ role: 'assistant', content: buildAiWelcomeMessage() }]
}

const showBatchImport = () => {
  batchTab.value = 'ai'
  batchJson.value = ''
  batchResult.value = null
  resetAiImportState()
  batchVisible.value = true
}

const handleAiGenerate = async () => {
  const text = aiInput.value.trim()
  if (!text) {
    ElMessage.warning('请输入导入需求描述')
    return
  }
  const nextMessages = [...aiMessages.value, { role: 'user', content: text } as AiImportMessage]
  aiMessages.value = nextMessages
  aiInput.value = ''
  aiLoading.value = true
  try {
    const res = await parseAiImportUsers(nextMessages)
    const data = res.data
    aiReady.value = data.ready
    aiPreviewUsers.value = data.users || []
    aiMissingFields.value = data.missingFields || []
    aiAssumptions.value = data.assumptions || []
    aiRequiredFields.value = data.requiredFields?.length ? data.requiredFields : ['username', 'realName', 'role']
    aiOptionalFields.value = data.optionalFields?.length
      ? data.optionalFields
      : ['class', 'password', 'phone', 'email', 'status']
    if (data.assistantReply) {
      aiMessages.value = [...nextMessages, { role: 'assistant', content: data.assistantReply }]
    }
  } catch {
    aiMessages.value = nextMessages
  } finally {
    aiLoading.value = false
  }
}

const submitBatchUsers = async (users: Record<string, unknown>[]) => {
  batchSaving.value = true
  try {
    const res = await batchImportUsers(users)
    batchResult.value = res.data
    ElMessage.success(`导入完成：成功 ${batchResult.value?.success || 0} 条，失败 ${batchResult.value?.failed || 0} 条`)
    reload()
  } catch {
    // handled by interceptor
  } finally {
    batchSaving.value = false
  }
}

const handleBatchImport = async () => {
  const text = batchJson.value.trim()
  if (!text) {
    ElMessage.warning('请输入 JSON 数组')
    return
  }
  let users: Record<string, unknown>[]
  try {
    const parsed: unknown = JSON.parse(text)
    if (!Array.isArray(parsed) || !parsed.every((item) => typeof item === 'object' && item !== null)) {
      throw new Error('输入必须是由对象组成的 JSON 数组')
    }
    users = parsed as Record<string, unknown>[]
  } catch (e: unknown) {
    ElMessage.error('JSON 格式错误：' + (e instanceof Error ? e.message : '未知错误'))
    return
  }
  await submitBatchUsers(users)
}

const handleAiBatchImport = async () => {
  if (!aiReady.value || !aiPreviewUsers.value.length) {
    ElMessage.warning('请先让 AI 生成可导入的账号预览')
    return
  }
  const users = aiPreviewUsers.value.map((item) => ({
    username: item.username,
    realName: item.realName,
    roleId: item.roleId,
    classId: item.classId,
    password: item.password,
    phone: item.phone,
    email: item.email,
    status: item.status,
  }))
  await submitBatchUsers(users)
}

const loadOptions = async () => {
  try {
    const [roleRes, classRes] = await Promise.all([getRoles(), getClasses()])
    roles.value = roleRes.data?.records || roleRes.data || []
    // 后端返回 SysDict { id, dictLabel, dictValue }，映射为前端所需的 className
    classes.value = (classRes.data || []).map((item: { id: number; dictLabel: string }) => ({
      id: item.id,
      className: item.dictLabel,
    }))
  } catch {
    // fallback
  }
}

onMounted(() => {
  reload()
  loadOptions()
})
</script>

<style scoped>
.user-page {
  max-width: 1240px;
  margin: 0 auto;
}

.user-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
  gap: 8px;
}

.user-actions :deep(.el-button) {
  min-width: 0;
  margin-left: 0;
  padding-inline: 14px;
}

.batch-import-tabs :deep(.el-tabs__content) {
  min-height: 420px;
}

.ai-import-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 1fr);
  gap: 16px;
}

.ai-import-chat,
.ai-import-preview {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  background: #fff;
  padding: 16px;
}

.ai-import-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  color: #64748b;
  font-size: 13px;
}

.ai-import-messages {
  height: 240px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
  padding-right: 4px;
}

.ai-import-message {
  max-width: 88%;
  border-radius: 16px;
  padding: 12px 14px;
}

.ai-import-message p {
  margin: 6px 0 0;
  line-height: 1.6;
  white-space: pre-wrap;
}

.ai-import-message__role {
  font-size: 12px;
  font-weight: 600;
}

.ai-import-message--assistant {
  align-self: flex-start;
  background: #f8fafc;
  color: #1e293b;
}

.ai-import-message--user {
  align-self: flex-end;
  background: #7f1d1d;
  color: #fff;
}

.ai-import-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

.ai-import-hint {
  margin-bottom: 12px;
  color: #64748b;
  line-height: 1.6;
}

.ai-import-hint p,
.ai-import-summary h4,
.ai-import-preview__header h4 {
  margin: 0;
}

.ai-import-summary {
  margin-bottom: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fafc;
}

.ai-import-summary ul {
  margin: 8px 0 0;
  padding-left: 18px;
  color: #475569;
}

.ai-import-preview__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.batch-result {
  margin-top: 12px;
  padding: 12px 16px;
  border-radius: 14px;
  background: #fff8f1;
}

@media (max-width: 900px) {
  .ai-import-panel {
    grid-template-columns: 1fr;
  }
}
</style>
