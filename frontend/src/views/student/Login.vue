<template>
  <div class="login-page">
    <div class="login-shell reveal-up">
      <section class="login-aside">
        <span class="page-kicker">Exhibition Access</span>
        <h1 class="login-title">让作品以更好的方式被看见，也让每一次提交更像一次正式呈现。</h1>
        <p class="login-copy">
          平台将公共展览、学生创作、教师评审与后台归档整合为同一套视觉与工作流。登录后，你会进入与当前角色相匹配的工作界面。
        </p>

        <div class="login-metrics">
          <div class="hero-metric">
            <span class="hero-metric__label">Public</span>
            <span class="hero-metric__value">展览浏览</span>
          </div>
          <div class="hero-metric">
            <span class="hero-metric__label">Student</span>
            <span class="hero-metric__value">作品整理</span>
          </div>
          <div class="hero-metric">
            <span class="hero-metric__label">Teacher</span>
            <span class="hero-metric__value">匿名评审</span>
          </div>
          <div class="hero-metric">
            <span class="hero-metric__label">Admin</span>
            <span class="hero-metric__value">档案治理</span>
          </div>
        </div>

        <router-link to="/works" class="login-link"> 先进入公共展廊 </router-link>
      </section>

      <section class="login-panel">
        <div class="login-panel__tools">
          <AppThemeToggle />
        </div>

        <div class="login-panel__body">
          <p class="login-panel__eyebrow">User Access</p>
          <h2>账户登录</h2>
          <p class="login-panel__subtitle">请输入学号或用户名与密码。首次登录后将进入改密流程。</p>

          <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="学号 / 用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
            </el-form-item>
            <div class="login-panel__row">
              <el-checkbox v-model="form.remember">记住登录状态</el-checkbox>
              <span class="login-panel__tip">首次登录请使用初始密码</span>
            </div>
            <el-form-item>
              <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin"> 登 录 </el-button>
            </el-form-item>
            <div class="login-panel__register">
              <span>还没有账号？</span>
              <router-link to="/register" class="register-now">立即注册</router-link>
            </div>
          </el-form>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import AppThemeToggle from '@/components/AppThemeToggle.vue'
import { useAuthStore } from '@/app/session/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  remember: false,
})

const rules = {
  username: [{ required: true, message: '请输入学号或用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

function getRedirectPath(roleCode: string): string {
  const code = roleCode.replace('ROLE_', '').toLowerCase()
  switch (code) {
    case 'admin':
      return '/admin/dashboard'
    case 'teacher':
      return '/teacher/dashboard'
    case 'student':
      return '/student/home'
    default:
      return '/student/home'
  }
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.login(form.username, form.password, form.remember)

    if (authStore.userInfo?.firstLogin) {
      ElMessage.warning('首次登录，请先修改密码')
      router.push('/change-password')
      return
    }

    const roleCode = authStore.userInfo?.roleCode || 'student'
    const path = getRedirectPath(roleCode)
    ElMessage.success('登录成功')
    router.push(path)
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '登录失败，请检查账号密码'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-shell {
  width: min(1160px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 420px);
  border: 1px solid var(--border-subtle);
  border-radius: 36px;
  overflow: hidden;
  background: color-mix(in srgb, var(--surface-elevated) 88%, transparent);
  box-shadow: var(--shadow-lg);
}

.login-aside {
  padding: 48px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 28px;
  background:
    linear-gradient(155deg, color-mix(in srgb, var(--brand-soft) 88%, transparent), transparent 48%),
    linear-gradient(180deg, color-mix(in srgb, var(--card-bg) 78%, transparent), transparent);
}

.login-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(34px, 5vw, 58px);
  line-height: 1.08;
}

.login-copy {
  margin: 0;
  max-width: 620px;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.95;
}

.login-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.login-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  min-height: 46px;
  padding: 0 18px;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  color: var(--text-primary);
  transition:
    transform var(--transition-base),
    background-color var(--transition-fast);
}

.login-link:hover {
  transform: translateY(-1px);
  background: color-mix(in srgb, var(--brand-soft) 78%, transparent);
}

.login-panel {
  display: flex;
  flex-direction: column;
  padding: 22px;
  background: color-mix(in srgb, var(--card-bg) 88%, transparent);
}

.login-panel__tools {
  display: flex;
  justify-content: flex-end;
}

.login-panel__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 24px 18px 18px;
}

.login-panel__eyebrow {
  margin: 0 0 12px;
  color: var(--text-muted);
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.login-panel__body h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 34px;
  line-height: 1.1;
}

.login-panel__subtitle {
  margin: 10px 0 28px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.8;
}

.login-panel__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: -4px 0 20px;
}

.login-panel__tip {
  color: var(--text-muted);
  font-size: 12px;
}

.login-btn {
  width: 100%;
}

.login-panel__register {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: -8px;
  color: var(--text-muted);
  font-size: 14px;
}

.register-now {
  color: var(--el-color-primary);
  text-decoration: none;
  font-weight: 500;
}

.register-now:hover {
  text-decoration: underline;
}

@media (max-width: 960px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-aside,
  .login-panel {
    padding: 28px;
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 14px;
  }

  .login-aside,
  .login-panel {
    padding: 20px;
  }

  .login-metrics {
    grid-template-columns: 1fr;
  }

  .login-panel__row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
