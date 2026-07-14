<template>
  <div class="workspace-page work-submit">
    <section class="workspace-section workspace-section--soft reveal-up">
      <div class="workspace-intro">
        <div class="workspace-intro__body">
          <span class="workspace-intro__eyebrow">Submission Studio</span>
          <h2 class="workspace-intro__title">{{ pageTitle }}</h2>
          <p class="workspace-intro__summary">
            以编辑版式整理作品名称、摘要、材料与团队信息，让提交内容既适合审核，也适合后续展示。
          </p>
        </div>
      </div>
    </section>

    <section class="workspace-section reveal-up reveal-delay-1">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        :disabled="isView"
        label-width="110px"
        label-position="right"
        size="large"
        class="workspace-form"
      >
        <section class="workspace-form-section">
          <div class="workspace-form-section__header">
            <h3 class="workspace-form-section__title">基本信息</h3>
            <p class="workspace-form-section__desc">先定义作品的标题、摘要与核心技术标签，为评审建立第一层阅读线索。</p>
          </div>

          <el-form-item label="作品名称" prop="title">
            <el-input v-model="form.title" placeholder="请输入作品名称" maxlength="100" show-word-limit />
          </el-form-item>

          <el-form-item label="作品简介" prop="summary">
            <el-input
              v-model="form.summary"
              type="textarea"
              :rows="4"
              placeholder="请简要描述作品的功能、用途和亮点"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <div class="workspace-form-grid">
            <el-form-item label="技术栈" prop="techStack">
              <el-select
                v-model="form.techStack"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="请选择或输入技术栈（可多选）"
                style="width: 100%"
              >
                <el-option
                  v-for="tag in tagOptions"
                  :key="tag.dictLabel"
                  :label="tag.dictLabel"
                  :value="tag.dictLabel"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="指导教师" prop="advisor">
              <el-input v-model="form.advisor" placeholder="请输入指导教师姓名" />
            </el-form-item>
          </div>

          <el-form-item label="封面图片" prop="coverUrl">
            <el-upload
              :disabled="isView"
              :show-file-list="false"
              :before-upload="handleCoverUpload"
              accept="image/png,image/jpeg,image/gif"
            >
              <template #trigger>
                <div v-if="!form.coverUrl" class="cover-upload">
                  <el-icon :size="32"><Plus /></el-icon>
                  <span>上传封面</span>
                </div>
                <el-image v-else :src="form.coverUrl" class="cover-preview" fit="cover" />
              </template>
            </el-upload>
          </el-form-item>
        </section>

        <section class="workspace-form-section">
          <div class="workspace-form-section__header">
            <h3 class="workspace-form-section__title">附件资料</h3>
            <p class="workspace-form-section__desc">
              整理压缩包、图像、视频与运行说明，让审核和展示都有充足的阅读材料。
            </p>
          </div>

          <el-form-item label="上传文件" prop="attachments">
            <div class="attachment-area">
              <el-upload
                v-model:file-list="fileList"
                :disabled="isView"
                :before-upload="beforeFileUpload"
                :on-remove="handleFileRemove"
                multiple
                list-type="text"
              >
                <el-button v-if="!isView" type="primary" plain>
                  <el-icon><Upload /></el-icon>选择文件
                </el-button>
                <template #tip>
                  <div class="el-upload__tip">支持 zip/rar/7z/jpg/png/gif/mp4/pdf，图片≤10MB，视频≤1.5GB</div>
                </template>
              </el-upload>
            </div>
          </el-form-item>

          <el-form-item label="服务器地址" prop="previewUrl">
            <el-input v-model="form.previewUrl" placeholder="请输入服务器 IP、域名或 http/https 完整地址" clearable />
          </el-form-item>

          <el-form-item label="运行说明" prop="runDescription">
            <el-input
              v-model="form.runDescription"
              type="textarea"
              :rows="4"
              placeholder="请描述作品的运行环境要求、安装步骤和启动方式（可选）"
            />
          </el-form-item>
        </section>

        <section class="workspace-form-section">
          <div class="workspace-form-section__header">
            <h3 class="workspace-form-section__title">团队成员</h3>
            <p class="workspace-form-section__desc">明确主创与成员信息，保持提交名单与展示信息的一致性。</p>
          </div>

          <el-form-item label="成员列表">
            <div class="member-area">
              <div v-for="(member, idx) in form.members" :key="idx" class="member-row">
                <el-select v-model="member.isLeader" class="member-role" @change="handleLeaderChange(idx)">
                  <el-option :value="true" label="队长" />
                  <el-option :value="false" label="成员" />
                </el-select>
                <el-input v-model="member.studentName" placeholder="姓名" class="member-input" />
                <el-input v-model="member.studentNo" placeholder="学号" class="member-input" />
                <el-button v-if="!isView" type="danger" :icon="Delete" circle @click="removeMember(idx)" />
              </div>
              <el-button v-if="!isView" type="primary" plain @click="addMember">
                <el-icon><Plus /></el-icon>添加成员
              </el-button>
            </div>
          </el-form-item>
        </section>

        <div class="form-actions">
          <el-button v-if="!isView" type="primary" size="large" :loading="submitting" @click="handleSaveAndSubmit">
            {{ submitting ? '内容安全检测中...' : '提交审核' }}
          </el-button>
          <el-button v-if="!isView" size="large" :loading="saving" @click="handleSaveDraft"> 保存草稿 </el-button>
          <el-button size="large" @click="router.back()">{{ isView ? '返回' : '取消' }}</el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type UploadFile, type UploadFiles, type UploadUserFile } from 'element-plus'
import { Plus, Delete, Upload } from '@element-plus/icons-vue'
import {
  createWork,
  updateWork,
  submitWork,
  getWorkDetail,
  uploadFile,
  type WorkForm,
  type WorkAttachment,
} from '@/api/student/work'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const saving = ref(false)
const fileList = ref<UploadUserFile[]>([])
const _pendingUploadNames = new Set<string>()
const hasWorkId = computed(() => Boolean(getRouteWorkId()))
const isEdit = computed(() => route.name === 'WorkEdit' || (hasWorkId.value && route.name !== 'WorkView'))
const isView = computed(() => route.name === 'WorkView')
const pageTitle = computed(() => {
  if (isView.value) return '作品详情'
  return isEdit.value ? '编辑作品' : '提交作品'
})

const tagOptions = ref<{ id: number; dictLabel: string }[]>([])

const form = reactive<WorkForm>({
  title: '',
  summary: '',
  techStack: [] as unknown as string,
  advisor: '',
  coverUrl: '',
  videoUrl: '',
  previewUrl: '',
  runDescription: '',
  batchId: null,
  attachments: [],
  members: [
    {
      studentName: '',
      studentNo: '',
      className: '',
      isLeader: true,
    },
  ],
})

const rules = {
  title: [
    { required: true, message: '请输入作品名称', trigger: 'blur' },
    { min: 2, max: 100, message: '作品名称长度在 2 到 100 个字符', trigger: 'blur' },
  ],
  summary: [{ required: true, message: '请输入作品简介', trigger: 'blur' }],
  techStack: [
    { required: true, message: '请选择技术栈', trigger: 'change' },
    { type: 'array', min: 1, message: '至少选择一个技术栈', trigger: 'change' },
  ],
  previewUrl: [
    { required: true, message: '请输入服务器访问地址', trigger: 'blur' },
    { validator: validatePreviewUrl, trigger: 'blur' },
  ],
}

function validatePreviewUrl(_rule: unknown, value: string, callback: (error?: Error) => void) {
  const url = normalizePreviewUrl(value)
  if (!url) {
    callback(new Error('请输入服务器访问地址'))
    return
  }
  try {
    const parsed = new URL(url)
    if (!['http:', 'https:'].includes(parsed.protocol)) {
      callback(new Error('服务器访问地址仅支持 IP、域名、http:// 或 https:// 地址'))
      return
    }
    if (['localhost', '127.0.0.1', '0.0.0.0', '[::1]'].includes(parsed.hostname.toLowerCase())) {
      callback(new Error('服务器访问地址不能使用本地地址'))
      return
    }
    callback()
  } catch {
    callback(new Error('服务器访问地址格式不正确'))
  }
}

function getRouteWorkId() {
  const id = route.params.id
  return typeof id === 'string' ? id : Array.isArray(id) ? id[0] : ''
}

async function loadWork(id: string | number) {
  try {
    const res = await getWorkDetail(id)
    const data = res.data
    Object.assign(form, {
      title: data.title,
      summary: data.summary,
      techStack: data.techStack ? data.techStack.split(',').map((s: string) => s.trim()) : [],
      advisor: data.advisor,
      coverUrl: data.coverUrl,
      videoUrl: resolveUploadedVideoUrl(data.attachments) || data.videoUrl || '',
      previewUrl: data.previewUrl || '',
      runDescription: data.runDescription,
      batchId: data.batchId || null,
      members: data.members?.length ? data.members : form.members,
    })
    // 后端返回的附件仅用于初始化（如已有附件则不覆盖已上传的）
    if (data.attachments?.length) {
      form.attachments = data.attachments
    }
    fileList.value = (data.attachments || []).map((a: WorkAttachment, index: number) => ({
      uid: index + 1,
      name: a.fileName,
      url: a.fileUrl,
      status: 'success' as const,
    }))
  } catch {
    ElMessage.error('加载作品信息失败')
  }
}

async function loadTags() {
  try {
    const res = await request.get<{ id: number; dictLabel: string }[]>('/api/public/tags')
    tagOptions.value = res.data || []
  } catch {
    tagOptions.value = []
  }
}

onMounted(() => {
  loadTags()
  // 从待办页面传入 batchId，自动设置
  const batchIdFromQuery = route.query?.batchId
  if (batchIdFromQuery) {
    // const id = Number(batchIdFromQuery)  // 雪花 ID 直接用字符串，防止精度丢失
    form.batchId = Array.isArray(batchIdFromQuery) ? batchIdFromQuery[0] : batchIdFromQuery
  }
  const workId = getRouteWorkId()
  if (workId) {
    loadWork(workId)
  }
})

function addMember() {
  form.members.push({
    studentName: '',
    studentNo: '',
    className: '',
    isLeader: false,
  })
}

function removeMember(idx: number) {
  if (form.members.length <= 1) {
    ElMessage.warning('至少需要一名成员')
    return
  }
  form.members.splice(idx, 1)
}

function handleLeaderChange(changedIdx: number) {
  if (form.members[changedIdx].isLeader) {
    form.members.forEach((m, idx) => {
      if (idx !== changedIdx) m.isLeader = false
    })
  }
}

async function handleCoverUpload(file: File): Promise<boolean> {
  try {
    const res = await uploadFile(file, getRouteWorkId() || undefined)
    form.coverUrl = res.data.url
  } catch {
    ElMessage.error('封面上传失败')
  }
  return false
}

async function beforeFileUpload(file: File): Promise<boolean> {
  const whiteList = ['zip', 'rar', '7z', 'jpg', 'png', 'gif', 'mp4', 'pdf']
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ext || !whiteList.includes(ext)) {
    ElMessage.error('不支持的文件格式')
    return false
  }
  const maxSize =
    ext === 'mp4' ? 1536 * 1024 * 1024 : ['jpg', 'png', 'gif'].includes(ext) ? 10 * 1024 * 1024 : 500 * 1024 * 1024
  if (file.size > maxSize) {
    const message =
      ext === 'mp4'
        ? '视频文件不能超过1.5GB'
        : ['jpg', 'png', 'gif'].includes(ext)
          ? '图片文件不能超过10MB'
          : '压缩包文件不能超过200MB'
    ElMessage.error(message)
    return false
  }
  try {
    const res = await uploadFile(file, getRouteWorkId() || undefined)
    const uploadedFile = {
      uid: Date.now(),
      name: file.name,
      url: res.data.url,
      status: 'success' as const,
    }
    form.attachments.push({
      id: res.data.id,
      fileName: file.name,
      fileType: ext,
      fileSize: file.size,
      fileUrl: res.data.url,
    })
    if (ext === 'mp4') {
      form.videoUrl = res.data.url
    }
    fileList.value = [...fileList.value, uploadedFile]
    ElMessage.success('文件已上传到服务器，保存草稿或提交审核后会绑定到作品')
  } catch {
    ElMessage.error('上传失败')
  }
  _pendingUploadNames.add(file.name)
  return false
}

function handleFileRemove(file: UploadFile, list: UploadFiles) {
  fileList.value = list
  // el-upload 在 beforeFileUpload 返回 false 后会触发一次自动删除，跳过
  if (_pendingUploadNames.has(file.name)) {
    _pendingUploadNames.delete(file.name)
    return
  }
  const idx = form.attachments.findIndex((a) => a.fileName === file.name)
  if (idx !== -1) {
    const [removed] = form.attachments.splice(idx, 1)
    if (removed?.fileType?.toLowerCase?.() === 'mp4' && form.videoUrl === removed.fileUrl) {
      form.videoUrl = resolveUploadedVideoUrl(form.attachments)
    }
  }
}

function buildSubmitData(): WorkForm {
  form.videoUrl = resolveUploadedVideoUrl(form.attachments)
  form.previewUrl = normalizePreviewUrl(form.previewUrl)
  // 技术栈数组转逗号分隔字符串（后端兼容格式）
  const techStackStr = Array.isArray(form.techStack) ? form.techStack.join(',') : form.techStack
  return {
    ...form,
    techStack: techStackStr,
    attachments: form.attachments,
    members: form.members.filter((m) => m.studentName && m.studentNo),
  }
}

function hasVideoFile() {
  return form.attachments.some((file) => file.fileType?.toLowerCase?.() === 'mp4')
}

function resolveUploadedVideoUrl(attachments?: WorkAttachment[]) {
  return attachments?.find((file) => file.fileType?.toLowerCase?.() === 'mp4')?.fileUrl || ''
}

function normalizePreviewUrl(value?: string) {
  const url = value?.trim()
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  return `http://${url}`
}

async function handleSaveDraft() {
  saving.value = true
  try {
    const data = buildSubmitData()
    if (hasWorkId.value) {
      await updateWork(getRouteWorkId(), data)
    } else {
      await createWork(data)
    }
    ElMessage.success('已保存草稿')
    router.push('/student/works')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    saving.value = false
  }
}

async function handleSaveAndSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!hasVideoFile()) {
    ElMessage.error('提交审核前请上传演示视频文件')
    return
  }
  submitting.value = true
  try {
    const data = buildSubmitData()
    let id = getRouteWorkId()
    if (hasWorkId.value) {
      await updateWork(id, data)
    } else {
      const res = await createWork(data)
      id = res.data
    }
    await submitWork(id)
    ElMessage.success('提交成功，请等待管理员审核')
    router.push('/student/works')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.work-submit {
  max-width: 1040px;
  margin: 0 auto;
}

.cover-upload {
  width: 220px;
  height: 136px;
  border: 1px dashed var(--border-color);
  border-radius: 18px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-muted);
  background: color-mix(in srgb, var(--card-bg) 78%, transparent);
  cursor: pointer;
  transition:
    border-color var(--transition-fast),
    color var(--transition-fast),
    transform var(--transition-base);
}
.cover-upload:hover {
  border-color: var(--brand);
  color: var(--brand);
  transform: translateY(-1px);
}

.cover-preview {
  width: 220px;
  height: 136px;
  border-radius: 18px;
  cursor: pointer;
}

.attachment-area {
  width: 100%;
}
.member-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.member-row {
  display: grid;
  grid-template-columns: 90px repeat(3, minmax(0, 1fr)) auto;
  gap: 10px;
  align-items: center;
}

.member-role {
  width: 90px;
}
.member-input {
  flex: 1;
  min-width: 0;
}
.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding-top: 4px;
}

@media (max-width: 768px) {
  .member-row {
    grid-template-columns: 1fr;
  }

  .member-role {
    width: 100%;
  }
}
</style>
