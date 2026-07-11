<template>
  <div class="workspace-page role-page">
    <section class="workspace-section workspace-section--soft reveal-up">
      <div class="workspace-toolbar">
        <div class="workspace-toolbar__body">
          <h2 class="workspace-toolbar__title">角色权限</h2>
          <p class="workspace-toolbar__desc">把角色档案与菜单授权放进同一张工作台里，减少来回切页时的判断成本。</p>
          <div class="workspace-toolbar__meta">
            <span>角色数量 {{ roleTotal }}</span>
            <span>菜单节点 {{ allMenuIds.length }}</span>
            <span v-if="currentRole">当前角色 {{ currentRole.roleCode }}</span>
          </div>
        </div>
        <div class="workspace-toolbar__actions">
          <el-button type="primary" @click="openRoleDialog()">新增角色</el-button>
        </div>
      </div>
    </section>

    <div class="role-layout">
      <section class="workspace-section reveal-up reveal-delay-1 role-card">
        <div class="workspace-toolbar workspace-toolbar--tight">
          <div class="workspace-toolbar__body">
            <h3 class="workspace-toolbar__title">角色列表</h3>
            <p class="workspace-toolbar__desc">点击任意角色，即可在右侧查看并调整菜单授权。</p>
          </div>
        </div>

        <el-table :data="roles" v-loading="roleLoading" stripe highlight-current-row @row-click="handleRoleClick">
          <el-table-column prop="roleName" label="角色名称" width="120" />
          <el-table-column prop="roleCode" label="角色编码" width="150" />
          <el-table-column prop="description" label="描述" show-overflow-tooltip />
          <el-table-column label="操作" width="156" fixed="right">
            <template #default="{ row }">
              <div class="role-actions">
                <el-button size="small" plain @click.stop="openRoleDialog(row)">编辑</el-button>
                <el-button size="small" type="danger" @click.stop="handleDelete(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="workspace-pagination">
          <PaginationBar v-model:page="rolePage" v-model:size="roleSize" :total="roleTotal" @change="loadRoles" />
        </div>
      </section>

      <section class="workspace-section reveal-up reveal-delay-2 menu-card">
        <div class="workspace-toolbar workspace-toolbar--tight">
          <div class="workspace-toolbar__body">
            <h3 class="workspace-toolbar__title">菜单权限</h3>
            <p class="workspace-toolbar__desc">
              {{
                currentRole
                  ? '当前角色的菜单权限会立即在树中展开，勾选后可直接保存。'
                  : '先从左侧选中一个角色，再开始配置它的菜单访问范围。'
              }}
            </p>
          </div>
          <div class="workspace-toolbar__actions">
            <el-button :disabled="!currentRole" @click="handleClearMenus">清空</el-button>
            <el-button :disabled="!currentRole" @click="handleCheckAllMenus">全选</el-button>
            <el-button type="primary" :disabled="!currentRole" :loading="menuSaving" @click="handleSaveMenus">
              保存权限
            </el-button>
          </div>
        </div>

        <template v-if="currentRole">
          <div class="role-focus">
            <div class="role-focus__primary">
              <span class="role-focus__code">{{ currentRole.roleCode }}</span>
              <h4 class="role-focus__name">{{ currentRole.roleName }}</h4>
              <p class="role-focus__desc">{{ currentRole.description || '这个角色暂时还没有补充描述信息。' }}</p>
            </div>
            <div class="role-focus__stats">
              <div class="role-focus__stat">
                <label>已勾选节点</label>
                <strong>{{ menuSelectionCount }}</strong>
              </div>
              <div class="role-focus__stat">
                <label>总节点数</label>
                <strong>{{ allMenuIds.length }}</strong>
              </div>
            </div>
          </div>

          <div class="menu-tree-wrap">
            <el-tree
              ref="menuTreeRef"
              :data="menuTree"
              show-checkbox
              node-key="id"
              :props="{ label: 'menuName', children: 'children' }"
              default-expand-all
              highlight-current
              @check="syncMenuSelectionCount"
            />
          </div>
        </template>

        <div v-else class="menu-placeholder">
          <div class="menu-placeholder__box">
            <h4>请选择角色</h4>
            <p>从左侧角色列表中选中一个角色后，这里会展开它的菜单权限树，方便你集中分配与保存。</p>
          </div>
        </div>
      </section>
    </div>

    <el-dialog v-model="roleDialogVisible" :title="isEditRole ? '编辑角色' : '新增角色'" width="450px" destroy-on-close>
      <el-form :model="roleForm" label-width="80px" ref="roleFormRef" :rules="roleRules">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="roleForm.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="roleForm.roleCode" placeholder="如 ROLE_XXX" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" placeholder="角色描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitting" @click="handleRoleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { getRoles, createRole, updateRole, deleteRole, getRoleMenus, updateRoleMenus } from '@/api/admin/role'
import { getMenuTree } from '@/api/admin/menu'
import type { RoleItem } from '@/api/admin/role'
import type { MenuItem } from '@/api/admin/menu'
import type { ElTree } from 'element-plus'

const roleLoading = ref(false)
const roles = ref<RoleItem[]>([])
const roleTotal = ref(0)
const rolePage = ref(1)
const roleSize = ref(10)

const menuTree = ref<MenuItem[]>([])
const menuSaving = ref(false)
const menuTreeRef = ref<InstanceType<typeof ElTree> | null>(null)
const menuSelectionCount = ref(0)

const currentRole = ref<RoleItem | null>(null)

const roleDialogVisible = ref(false)
const isEditRole = ref(false)
const roleSubmitting = ref(false)
const roleFormRef = ref<FormInstance>()

const roleForm = reactive({
  id: undefined as number | undefined,
  roleName: '',
  roleCode: '',
  description: '',
})

const roleRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
}

const collectMenuIds = (nodes: MenuItem[]): number[] => {
  return nodes.flatMap((node) => [node.id, ...(node.children ? collectMenuIds(node.children) : [])])
}

const allMenuIds = computed(() => collectMenuIds(menuTree.value))

const syncMenuSelectionCount = () => {
  const checkedKeys = (menuTreeRef.value?.getCheckedKeys() as number[] | undefined) || []
  const halfKeys = (menuTreeRef.value?.getHalfCheckedKeys() as number[] | undefined) || []
  menuSelectionCount.value = checkedKeys.length + halfKeys.length
}

const loadRoles = async () => {
  roleLoading.value = true
  try {
    const res = await getRoles({ page: rolePage.value, size: roleSize.value, excludeSystem: true })
    roles.value = res.data?.records || []
    roleTotal.value = res.data?.total || 0
  } finally {
    roleLoading.value = false
  }
}

const loadMenuTree = async () => {
  try {
    const res = await getMenuTree()
    menuTree.value = res.data || []
  } catch {
    /* ignore */
  }
}

const handleRoleClick = async (row: RoleItem) => {
  currentRole.value = row
  await nextTick()
  menuTreeRef.value?.setCheckedKeys([])
  syncMenuSelectionCount()

  try {
    const res = await getRoleMenus(row.id)
    const checkedIds = (res.data as number[]) || []
    menuTreeRef.value?.setCheckedKeys(checkedIds)
    syncMenuSelectionCount()
  } catch {
    ElMessage.warning('获取角色菜单失败')
  }
}

const handleCheckAllMenus = () => {
  menuTreeRef.value?.setCheckedKeys(allMenuIds.value)
  syncMenuSelectionCount()
}

const handleClearMenus = () => {
  menuTreeRef.value?.setCheckedKeys([])
  syncMenuSelectionCount()
}

const handleSaveMenus = async () => {
  if (!currentRole.value) return
  menuSaving.value = true
  try {
    const checkedKeys = menuTreeRef.value?.getCheckedKeys() as number[]
    const halfKeys = menuTreeRef.value?.getHalfCheckedKeys() as number[]
    await updateRoleMenus(currentRole.value.id, [...checkedKeys, ...halfKeys])
    syncMenuSelectionCount()
    ElMessage.success('权限保存成功')
  } catch {
    // handled
  } finally {
    menuSaving.value = false
  }
}

const openRoleDialog = (row?: RoleItem) => {
  if (row) {
    isEditRole.value = true
    roleForm.id = row.id
    roleForm.roleName = row.roleName
    roleForm.roleCode = row.roleCode
    roleForm.description = row.description
  } else {
    isEditRole.value = false
    roleForm.id = undefined
    roleForm.roleName = ''
    roleForm.roleCode = ''
    roleForm.description = ''
  }
  roleDialogVisible.value = true
}

const handleRoleSubmit = async () => {
  const valid = await roleFormRef.value?.validate().catch(() => false)
  if (!valid) return

  roleSubmitting.value = true
  try {
    if (isEditRole.value && roleForm.id) {
      await updateRole(roleForm.id, {
        roleName: roleForm.roleName,
        roleCode: roleForm.roleCode,
        description: roleForm.description,
      })
      ElMessage.success('更新成功')
    } else {
      await createRole({ roleName: roleForm.roleName, roleCode: roleForm.roleCode, description: roleForm.description })
      ElMessage.success('创建成功')
    }
    roleDialogVisible.value = false
    loadRoles()
  } finally {
    roleSubmitting.value = false
  }
}

const handleDelete = async (row: RoleItem) => {
  try {
    await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '提示')
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    if (currentRole.value?.id === row.id) {
      currentRole.value = null
      menuSelectionCount.value = 0
    }
    loadRoles()
  } catch {
    // cancelled
  }
}

onMounted(() => {
  loadRoles()
  loadMenuTree()
})
</script>

<style scoped>
.role-page {
  max-width: 1240px;
  margin: 0 auto;
}

.role-layout {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.role-card {
  min-width: 0;
}

.menu-card {
  min-width: 0;
  min-height: 560px;
}

.role-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.role-actions :deep(.el-button) {
  min-width: 0;
  margin-left: 0;
  padding-inline: 14px;
}

.role-focus {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding: 18px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  background:
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--card-bg) 92%, transparent),
      color-mix(in srgb, var(--card-bg) 80%, transparent)
    ),
    linear-gradient(135deg, color-mix(in srgb, var(--brand-soft) 22%, transparent), transparent 52%);
}

.role-focus__primary {
  min-width: 0;
}

.role-focus__code {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--brand-soft) 72%, transparent);
  color: var(--brand);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.role-focus__name {
  margin: 14px 0 0;
  font-family: var(--font-display);
  font-size: 30px;
  line-height: 1.06;
}

.role-focus__desc {
  margin: 10px 0 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.role-focus__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(120px, 1fr));
  gap: 12px;
}

.role-focus__stat {
  min-width: 0;
  padding: 14px 16px;
  border: 1px solid var(--border-subtle);
  border-radius: 16px;
  background: color-mix(in srgb, var(--card-bg) 82%, transparent);
}

.role-focus__stat label {
  display: block;
  color: var(--text-muted);
  font-size: 12px;
}

.role-focus__stat strong {
  display: block;
  margin-top: 8px;
  font-family: var(--font-display);
  font-size: 28px;
  line-height: 1;
}

.menu-tree-wrap {
  max-height: 560px;
  overflow: auto;
  padding: 8px 6px 4px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--card-bg) 82%, transparent);
}

.menu-tree-wrap :deep(.el-tree) {
  background: transparent;
}

.menu-tree-wrap :deep(.el-tree-node__content) {
  height: 40px;
  border-radius: 12px;
}

.menu-tree-wrap :deep(.el-tree-node__content:hover) {
  background: color-mix(in srgb, var(--brand-soft) 70%, transparent);
}

.menu-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 380px;
}

.menu-placeholder__box {
  max-width: 360px;
  padding: 26px 28px;
  border: 1px dashed var(--border-color);
  border-radius: 22px;
  background: color-mix(in srgb, var(--card-bg) 76%, transparent);
  text-align: center;
}

.menu-placeholder__box h4 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 28px;
}

.menu-placeholder__box p {
  margin: 12px 0 0;
  color: var(--text-muted);
  line-height: 1.8;
}

@media (max-width: 900px) {
  .role-layout {
    grid-template-columns: 1fr;
  }

  .role-focus {
    flex-direction: column;
  }

  .role-focus__stats {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .role-focus__stats {
    grid-template-columns: 1fr;
  }
}
</style>
