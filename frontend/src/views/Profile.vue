<template>
  <div class="profile-page">
    <div class="profile-page__orb profile-page__orb--left" />
    <div class="profile-page__orb profile-page__orb--right" />

    <div class="profile-container workspace-page">
      <section class="workspace-section workspace-section--soft reveal-up profile-hero">
        <div class="workspace-toolbar">
          <div class="workspace-toolbar__body">
            <span class="page-kicker">Personal Archive</span>
            <h1 class="profile-hero__title">个人信息</h1>
            <p class="profile-hero__desc">
              在这里维护你的展示身份、联系信息与账户安全，让头像、资料与访问习惯始终保持最新。
            </p>
            <div class="profile-hero__meta">
              <span class="profile-pill">{{ infoForm.roleName || '当前角色' }}</span>
              <span v-if="infoForm.className" class="profile-pill">{{ infoForm.className }}</span>
              <span class="profile-pill profile-pill--soft">{{ infoForm.username || '未命名账号' }}</span>
            </div>
          </div>

          <div class="workspace-toolbar__actions">
            <el-button @click="goHome">返回主界面</el-button>
          </div>
        </div>
      </section>

      <div class="profile-grid">
        <el-card class="profile-card profile-card--identity reveal-up reveal-delay-1" shadow="never">
          <div class="avatar-section">
            <el-upload
              :show-file-list="false"
              :before-upload="handleAvatarUpload"
              accept="image/png,image/jpeg,image/gif"
            >
              <template #trigger>
                <el-avatar :size="96" :src="infoForm.avatar || undefined" class="profile-avatar">
                  {{ avatarFallback }}
                </el-avatar>
                <div class="avatar-hint">点击更换头像</div>
              </template>
            </el-upload>
          </div>

          <div class="profile-identity">
            <h2 class="profile-identity__name">{{ infoForm.realName || '未设置姓名' }}</h2>
            <p class="profile-identity__role">{{ infoForm.roleName || '未分配角色' }}</p>
            <div class="profile-identity__list">
              <div class="profile-identity__item">
                <label>用户名</label>
                <span>{{ infoForm.username || '未填写' }}</span>
              </div>
              <div class="profile-identity__item" v-if="infoForm.className">
                <label>班级</label>
                <span>{{ infoForm.className }}</span>
              </div>
              <div class="profile-identity__item">
                <label>手机号</label>
                <span>{{ infoForm.phone || '未填写手机号' }}</span>
              </div>
              <div class="profile-identity__item">
                <label>邮箱</label>
                <span>{{ infoForm.email || '未填写邮箱' }}</span>
              </div>
            </div>
          </div>
        </el-card>

        <div class="profile-main">
          <el-card class="profile-card reveal-up reveal-delay-2" shadow="never">
            <template #header>
              <div class="profile-card__header">
                <div>
                  <span class="card-title">资料编辑</span>
                  <p class="profile-card__subtitle">基础身份信息保持只读，联系方式与头像可以在这里更新。</p>
                </div>
              </div>
            </template>
            <el-form :model="infoForm" label-width="84px" size="large" class="profile-form">
              <div class="profile-form__grid">
                <el-form-item label="用户名">
                  <el-input v-model="infoForm.username" disabled />
                </el-form-item>
                <el-form-item label="真实姓名">
                  <el-input v-model="infoForm.realName" disabled />
                </el-form-item>
                <el-form-item label="角色">
                  <el-input v-model="infoForm.roleName" disabled />
                </el-form-item>
                <el-form-item label="班级" v-if="infoForm.className">
                  <el-input v-model="infoForm.className" disabled />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="infoForm.phone" placeholder="请输入手机号" />
                </el-form-item>
                <el-form-item label="邮箱">
                  <el-input v-model="infoForm.email" placeholder="请输入邮箱" />
                </el-form-item>
              </div>
              <div class="profile-form__actions">
                <el-button type="primary" :loading="savingInfo" @click="handleSaveInfo">保存修改</el-button>
              </div>
            </el-form>
          </el-card>

          <el-card class="profile-card reveal-up reveal-delay-3" shadow="never">
            <template #header>
              <div class="profile-card__header">
                <div>
                  <span class="card-title">修改密码</span>
                  <p class="profile-card__subtitle">建议定期更新密码，首次登录后的默认密码请尽快替换。</p>
                </div>
              </div>
            </template>
            <el-form
              ref="pwdFormRef"
              :model="pwdForm"
              :rules="pwdRules"
              label-width="100px"
              size="large"
              class="profile-form"
            >
              <el-form-item label="旧密码" prop="oldPassword">
                <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入旧密码" show-password />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="pwdForm.newPassword"
                  type="password"
                  placeholder="请输入新密码（至少6位）"
                  show-password
                />
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPwd">
                <el-input v-model="pwdForm.confirmPwd" type="password" placeholder="请再次输入新密码" show-password />
              </el-form-item>
              <div class="profile-form__actions">
                <el-button type="primary" :loading="savingPwd" @click="handleChangePwd">修改密码</el-button>
              </div>
            </el-form>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { useAuthStore } from '@/app/session/auth'
import { updateProfile, changePassword } from '@/api/student/auth'
import { uploadFile } from '@/api/student/work'

const router = useRouter()
const authStore = useAuthStore()
const pwdFormRef = ref<FormInstance>()
const savingInfo = ref(false)
const savingPwd = ref(false)
const avatarFallback = computed(() => infoForm.realName?.charAt(0) || '?')
const homePath = computed(() => {
  const roleCode = (authStore.userInfo?.roleCode || '').replace('ROLE_', '').toLowerCase()
  if (roleCode === 'admin') return '/admin/dashboard'
  if (roleCode === 'teacher') return '/teacher/dashboard'
  return '/student/home'
})

const infoForm = reactive({
  username: '',
  realName: '',
  roleName: '',
  className: '',
  avatar: '',
  phone: '',
  email: '',
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPwd: '',
})

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPwd: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

const loadUserInfo = () => {
  const info = authStore.userInfo
  if (info) {
    infoForm.username = info.username || ''
    infoForm.realName = info.realName || ''
    infoForm.roleName = info.roleName || ''
    infoForm.className = info.className || ''
    infoForm.avatar = info.avatar || ''
    infoForm.phone = info.phone || ''
    infoForm.email = info.email || ''
  }
}

const goHome = () => {
  router.push(homePath.value)
}

const handleAvatarUpload = async (file: File): Promise<boolean> => {
  try {
    const res = await uploadFile(file)
    const avatarUrl = res.data?.url || res.data
    infoForm.avatar = avatarUrl
    await updateProfile({ avatar: avatarUrl })
    ElMessage.success('头像更新成功')
    await authStore.fetchUserInfo()
  } catch {
    ElMessage.error('头像上传失败')
  }
  return false
}

const handleSaveInfo = async () => {
  savingInfo.value = true
  try {
    await updateProfile({
      phone: infoForm.phone,
      email: infoForm.email,
      avatar: infoForm.avatar || undefined,
    })
    ElMessage.success('保存成功')
    await authStore.fetchUserInfo()
  } catch {
    // handled by interceptor
  } finally {
    savingInfo.value = false
  }
}

const handleChangePwd = async () => {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  savingPwd.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    authStore.logout()
  } catch {
    // handled by interceptor
  } finally {
    savingPwd.value = false
  }
}

onMounted(() => {
  if (!authStore.userInfo) {
    authStore
      .fetchUserInfo()
      .then(loadUserInfo)
      .catch(() => router.push('/login'))
  } else {
    loadUserInfo()
  }
})
</script>

<style scoped>
.profile-page {
  position: relative;
  min-height: 100vh;
  padding: 36px 0 52px;
  overflow: hidden;
}

.profile-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(140deg, rgba(255, 255, 255, 0.26), transparent 28%),
    radial-gradient(circle at top left, color-mix(in srgb, var(--brand-soft) 82%, transparent), transparent 34%),
    radial-gradient(circle at 86% 12%, color-mix(in srgb, var(--accent-soft) 92%, transparent), transparent 24%);
  pointer-events: none;
}

.profile-page__orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(18px);
  opacity: 0.55;
  pointer-events: none;
}

.profile-page__orb--left {
  top: 92px;
  left: -48px;
  width: 180px;
  height: 180px;
  background: color-mix(in srgb, var(--brand-soft) 84%, white);
}

.profile-page__orb--right {
  top: 220px;
  right: -30px;
  width: 132px;
  height: 132px;
  background: color-mix(in srgb, var(--accent-soft) 82%, white);
}

.profile-container {
  position: relative;
  z-index: 1;
}

.profile-hero {
  overflow: hidden;
}

.profile-hero__title {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(34px, 5vw, 52px);
  line-height: 1.06;
}

.profile-hero__desc {
  max-width: 720px;
  margin: 12px 0 0;
  color: var(--text-secondary);
  line-height: 1.85;
}

.profile-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.profile-pill {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border: 1px solid var(--border-subtle);
  border-radius: 999px;
  background: color-mix(in srgb, var(--card-bg) 84%, transparent);
  color: var(--text-primary);
  font-size: 13px;
}

.profile-pill--soft {
  color: var(--text-secondary);
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(280px, 320px) minmax(0, 1fr);
  gap: 22px;
}

.profile-main {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.profile-card {
  height: fit-content;
}

.profile-card--identity {
  position: sticky;
  top: 28px;
  padding-top: 6px;
}

.profile-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.card-title {
  display: block;
  font-family: var(--font-display);
  font-size: 24px;
  line-height: 1.1;
}

.profile-card__subtitle {
  margin: 8px 0 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.8;
}

.avatar-section {
  text-align: center;
  margin-bottom: 22px;
}

.profile-avatar {
  cursor: pointer;
  border: 2px solid color-mix(in srgb, var(--brand) 18%, transparent);
  box-shadow: 0 18px 40px color-mix(in srgb, var(--brand) 12%, transparent);
  transition:
    transform var(--transition-base),
    border-color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.profile-avatar:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--brand) 50%, transparent);
  box-shadow: 0 24px 48px color-mix(in srgb, var(--brand) 18%, transparent);
}

.avatar-hint {
  margin-top: 10px;
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
}

.profile-identity {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.profile-identity__name {
  margin: 0;
  font-family: var(--font-display);
  font-size: 30px;
  line-height: 1.05;
  text-align: center;
}

.profile-identity__role {
  margin: -4px 0 0;
  color: var(--text-muted);
  text-align: center;
  font-size: 13px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.profile-identity__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.profile-identity__item {
  padding: 14px 16px;
  border: 1px solid var(--border-subtle);
  border-radius: 16px;
  background: color-mix(in srgb, var(--card-bg) 82%, transparent);
}

.profile-identity__item label {
  display: block;
  margin-bottom: 6px;
  color: var(--text-muted);
  font-size: 12px;
}

.profile-identity__item span {
  display: block;
  color: var(--text-primary);
  line-height: 1.6;
  word-break: break-all;
}

.profile-form {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.profile-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

.profile-form__actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
}

@media (max-width: 900px) {
  .profile-page {
    padding-block: 22px 36px;
  }

  .profile-grid {
    grid-template-columns: 1fr;
  }

  .profile-card--identity {
    position: static;
  }
}

@media (max-width: 640px) {
  .profile-form__grid {
    grid-template-columns: 1fr;
  }

  .profile-page__orb {
    opacity: 0.36;
  }
}
</style>
