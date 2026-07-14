<template>
  <div class="register-page">
    <div class="register-shell reveal-up">
      <section class="register-aside">
        <span class="page-kicker">Create Account</span>
        <h1 class="register-title">注册账号，加入菁选校园作品展示平台。</h1>
        <p class="register-copy">学生可提交与管理自己的作品，教师可参与评分评审。注册后请使用邮箱验证。</p>
        <router-link to="/login" class="register-link"> 已有账号？去登录 </router-link>
      </section>

      <section class="register-panel">
        <div class="register-panel__body">
          <p class="register-panel__eyebrow">Get Started</p>
          <h2>账号注册</h2>
          <p class="register-panel__subtitle">请填写以下信息完成注册。</p>

          <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleRegister">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="学号 / 工号" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="realName">
              <el-input v-model="form.realName" placeholder="真实姓名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="密码（至少6位）"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
            <el-form-item prop="confirmPwd">
              <el-input
                v-model="form.confirmPwd"
                type="password"
                placeholder="确认密码"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
            <el-form-item prop="roleId">
              <el-select v-model="form.roleId" placeholder="选择身份角色" style="width: 100%">
                <el-option :value="1" label="学生" />
                <el-option :value="2" label="教师" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="form.roleId === 1" prop="classId">
              <el-select v-model="form.classId" placeholder="选择班级" style="width: 100%">
                <el-option v-for="c in classes" :key="c.id" :label="c.dictLabel" :value="c.id" />
              </el-select>
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="form.email" placeholder="邮箱地址" :prefix-icon="Message" />
            </el-form-item>
            <el-form-item prop="verifyCode">
              <div class="verify-code-row">
                <el-input v-model="form.verifyCode" placeholder="邮箱验证码" />
                <el-button :disabled="sendingCode || cooldown > 0" @click="handleSendCode">
                  {{ cooldown > 0 ? `${cooldown}s` : '发送验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" class="register-btn" @click="handleRegister">
                注 册
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import request from '@/api/request'

const router = useRouter()
interface RegistrationClass {
  id: number
  dictLabel: string
}

interface RegistrationRequest {
  username: string
  realName: string
  password: string
  email: string
  verifyCode: string
  roleId?: number
  classId?: number
}

const formRef = ref<FormInstance>()
const loading = ref(false)
const sendingCode = ref(false)
const cooldown = ref(0)
const classes = ref<RegistrationClass[]>([])
let cooldownTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  username: '',
  realName: '',
  password: '',
  confirmPwd: '',
  roleId: undefined as number | undefined,
  classId: undefined as number | undefined,
  email: '',
  verifyCode: '',
})

const validateConfirmPwd = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入学号或工号', trigger: 'blur' },
    { min: 3, message: '至少3个字符', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPwd: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' },
  ],
  roleId: [{ required: true, message: '请选择身份角色', trigger: 'change' }],
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' },
  ],
}

const startCooldown = () => {
  cooldown.value = 60
  cooldownTimer = setInterval(() => {
    cooldown.value--
    if (cooldown.value <= 0) {
      if (cooldownTimer) clearInterval(cooldownTimer)
      cooldownTimer = null
    }
  }, 1000)
}

const handleSendCode = async () => {
  if (!form.email) {
    ElMessage.warning('请先输入邮箱地址')
    return
  }
  if (!form.roleId) {
    ElMessage.warning('请先选择身份角色')
    return
  }
  sendingCode.value = true
  try {
    await request.post('/api/auth/send-code', {
      email: form.email,
      roleId: form.roleId,
    })
    ElMessage.success('验证码已发送到邮箱')
    startCooldown()
  } catch {
    // handled by interceptor
  } finally {
    sendingCode.value = false
  }
}

const handleRegister = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const body: RegistrationRequest = {
      username: form.username,
      realName: form.realName,
      password: form.password,
      email: form.email,
      verifyCode: form.verifyCode,
      roleId: form.roleId,
    }
    if (form.roleId === 1) {
      body.classId = form.classId
    }
    await request.post('/api/auth/register', body)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

const loadClasses = async () => {
  try {
    const res = await request.get<RegistrationClass[]>('/api/v1/classes')
    classes.value = (res.data || []).map((item) => ({
      id: item.id,
      dictLabel: item.dictLabel,
    }))
  } catch {
    classes.value = []
  }
}

onMounted(() => {
  loadClasses()
})

onUnmounted(() => {
  if (cooldownTimer) clearInterval(cooldownTimer)
})
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.register-shell {
  width: min(1160px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(400px, 480px);
  border: 1px solid var(--border-subtle);
  border-radius: 36px;
  overflow: hidden;
  background: color-mix(in srgb, var(--surface-elevated) 88%, transparent);
  box-shadow: var(--shadow-lg);
}

.register-aside {
  padding: 48px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 28px;
  background:
    linear-gradient(155deg, color-mix(in srgb, var(--brand-soft) 88%, transparent), transparent 48%),
    linear-gradient(180deg, color-mix(in srgb, var(--card-bg) 78%, transparent), transparent);
}

.register-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(34px, 5vw, 58px);
  line-height: 1.08;
}

.register-copy {
  margin: 0;
  max-width: 620px;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.95;
}

.register-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  min-height: 46px;
  padding: 0 18px;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  color: var(--text-primary);
  text-decoration: none;
  transition:
    transform var(--transition-base),
    background-color var(--transition-fast);
}

.register-link:hover {
  transform: translateY(-1px);
  background: color-mix(in srgb, var(--brand-soft) 78%, transparent);
}

.register-panel {
  display: flex;
  flex-direction: column;
  padding: 32px;
  background: color-mix(in srgb, var(--card-bg) 88%, transparent);
}

.register-panel__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.register-panel__eyebrow {
  margin: 0 0 12px;
  color: var(--text-muted);
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.register-panel__body h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 34px;
  line-height: 1.1;
}

.register-panel__subtitle {
  margin: 10px 0 28px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.8;
}

.verify-code-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.verify-code-row .el-input {
  flex: 1;
}

.verify-code-row .el-button {
  flex-shrink: 0;
  min-width: 100px;
}

.register-btn {
  width: 100%;
}

@media (max-width: 960px) {
  .register-shell {
    grid-template-columns: 1fr;
  }

  .register-aside,
  .register-panel {
    padding: 28px;
  }
}

@media (max-width: 640px) {
  .register-page {
    padding: 14px;
  }

  .register-aside,
  .register-panel {
    padding: 20px;
  }
}
</style>
