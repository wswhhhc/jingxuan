<template>
  <div class="change-password-page">
    <div class="change-password-page__orb change-password-page__orb--left" />
    <div class="change-password-page__orb change-password-page__orb--right" />

    <div class="change-password-shell reveal-up">
      <section class="change-password-aside">
        <div class="change-password-aside__inner">
          <span class="page-kicker">Security Reset</span>
          <h1 class="change-password-title">首次登录后，先把默认密码换成只属于你的那一个。</h1>
          <p class="change-password-copy">
            这一步会为你的账号建立新的访问凭据。修改成功后，系统会要求你重新登录，之后再进入对应角色的工作界面。
          </p>

          <div class="change-password-notes">
            <div class="change-password-note">
              <span class="change-password-note__label">Required</span>
              <span class="change-password-note__value">旧密码 + 新密码</span>
            </div>
            <div class="change-password-note">
              <span class="change-password-note__label">Advice</span>
              <span class="change-password-note__value">至少 6 位，避免继续使用初始密码</span>
            </div>
          </div>
        </div>
      </section>

      <section class="change-password-panel">
        <div class="change-password-panel__tools">
          <AppThemeToggle />
        </div>

        <div class="change-password-panel__body">
          <p class="change-password-panel__eyebrow">Account Security</p>
          <h2>修改密码</h2>
          <p class="change-password-panel__subtitle">首次登录或出于安全要求，需要先完成密码重置。</p>

          <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleSubmit">
            <el-form-item prop="oldPassword">
              <el-input v-model="form.oldPassword" type="password" placeholder="旧密码" show-password />
            </el-form-item>
            <el-form-item prop="newPassword">
              <el-input v-model="form.newPassword" type="password" placeholder="新密码（至少6位）" show-password />
            </el-form-item>
            <el-form-item prop="confirmPwd">
              <el-input v-model="form.confirmPwd" type="password" placeholder="确认新密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" class="change-password-btn" @click="handleSubmit">
                确认修改
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import AppThemeToggle from '@/components/AppThemeToggle.vue'
import { useAuthStore } from '@/app/session/auth'
import { changePassword } from '@/api/student/auth'

const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPwd: '',
})

const rules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPwd: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    authStore.logout()
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.change-password-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: hidden;
}

.change-password-page__orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(18px);
  opacity: 0.6;
  pointer-events: none;
}

.change-password-page__orb--left {
  top: 72px;
  left: -42px;
  width: 200px;
  height: 200px;
  background: color-mix(in srgb, var(--brand-soft) 84%, white);
}

.change-password-page__orb--right {
  right: -26px;
  bottom: 82px;
  width: 150px;
  height: 150px;
  background: color-mix(in srgb, var(--accent-soft) 84%, white);
}

.change-password-shell {
  position: relative;
  z-index: 1;
  width: min(1100px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 430px);
  border: 1px solid var(--border-subtle);
  border-radius: 36px;
  overflow: hidden;
  background: color-mix(in srgb, var(--surface-elevated) 88%, transparent);
  box-shadow: var(--shadow-lg);
}

.change-password-aside {
  padding: 48px;
  display: flex;
  align-items: stretch;
  background:
    linear-gradient(155deg, color-mix(in srgb, var(--brand-soft) 86%, transparent), transparent 48%),
    linear-gradient(180deg, color-mix(in srgb, var(--card-bg) 78%, transparent), transparent);
}

.change-password-aside__inner {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 28px;
}

.change-password-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(32px, 4.8vw, 54px);
  line-height: 1.08;
  color: var(--text-primary);
}

.change-password-copy {
  margin: 0;
  max-width: 620px;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.95;
}

.change-password-notes {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.change-password-note {
  padding: 18px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--card-bg) 76%, transparent);
}

.change-password-note__label {
  display: block;
  margin-bottom: 8px;
  color: var(--text-muted);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.change-password-note__value {
  font-family: var(--font-display);
  font-size: 24px;
  line-height: 1.2;
  color: var(--text-primary);
}

.change-password-panel {
  display: flex;
  flex-direction: column;
  padding: 22px;
  background: color-mix(in srgb, var(--card-bg) 88%, transparent);
}

.change-password-panel__tools {
  display: flex;
  justify-content: flex-end;
}

.change-password-panel__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 24px 18px 18px;
}

.change-password-panel__eyebrow {
  margin: 0 0 12px;
  color: var(--text-muted);
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.change-password-panel__body h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 34px;
  line-height: 1.1;
  color: var(--text-primary);
}

.change-password-panel__subtitle {
  margin: 10px 0 28px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.8;
}

.change-password-btn {
  width: 100%;
}

@media (max-width: 960px) {
  .change-password-shell {
    grid-template-columns: 1fr;
  }

  .change-password-aside,
  .change-password-panel {
    padding: 28px;
  }
}

@media (max-width: 640px) {
  .change-password-page {
    padding: 14px;
  }

  .change-password-aside,
  .change-password-panel {
    padding: 20px;
  }

  .change-password-notes {
    grid-template-columns: 1fr;
  }
}
</style>
