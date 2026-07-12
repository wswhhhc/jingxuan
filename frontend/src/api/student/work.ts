import request from '../request'
import { adaptPageResult, adaptWorkVO, toBackendStatus } from '../workAdapter'

/* ============ 类型定义（前端使用） ============ */

export interface WorkAttachment {
  id?: string | number
  workId?: string | number
  fileName: string
  fileType: string
  fileSize?: number
  fileUrl: string
}

export interface WorkMember {
  id?: string | number
  workId?: string | number
  studentId?: string | number
  studentName: string
  studentNo: string
  className: string
  isLeader: boolean
  avatar?: string
}

export interface WorkForm {
  id?: string | number
  title: string
  summary: string
  techStack: string | string[]
  advisor: string
  coverUrl: string
  videoUrl: string
  previewUrl: string
  runDescription: string
  members: WorkMember[]
  attachments: WorkAttachment[]
  batchId?: string | null
}

export interface WorkItem {
  id: string | number
  title: string
  summary: string
  techStack: string | string[]
  advisor: string
  coverUrl: string
  videoUrl: string
  previewUrl: string
  runDescription: string
  status: 'draft' | 'submitted' | 'rejected' | 'approved'
  statusLabel?: string
  rejectReason?: string
  submitterId: string | number
  submitterName: string
  submitTime?: string
  createTime: string
  updateTime: string
  attachments: WorkAttachment[]
  members: WorkMember[]
  publishStatus?: 'unpublished' | 'published' | 'offline'
  featured?: number
  score?: number
  rank?: number
  avgInnovation?: string
  avgDifficulty?: string
  avgCompletion?: string
  avgPracticality?: string
  teacherCount?: number
  likeCount?: number
  viewCount?: number
  liked?: boolean
  tags?: string[]
}

export interface WorkListParams {
  page?: number
  pageSize?: number
  status?: string
  keyword?: string
}

/* ============ 后端响应 → 前端 WorkItem 适配 ============ */

/**
 * 适配后端 WorkListVO / WorkDetailVO 为前端 WorkItem
 */

/* ============ 前端表单 → 后端请求体适配 ============ */

function toCreateRequest(form: WorkForm) {
  const attachmentIds = form.attachments.filter((a) => a.id).map((a) => a.id)
  const uploadedVideoUrl = getUploadedVideoUrl(form)
  return {
    title: form.title,
    summary: form.summary,
    techStack: form.techStack,
    advisor: form.advisor,
    coverUrl: form.coverUrl,
    videoUrl: uploadedVideoUrl,
    previewUrl: form.previewUrl,
    runDesc: form.runDescription,
    batchId: form.batchId || undefined,
    members: form.members.map((m) => ({
      studentName: m.studentName,
      studentNo: m.studentNo,
      className: m.className,
      isLeader: m.isLeader ? 1 : 0,
    })),
    attachmentIds,
  }
}

function toUpdateRequest(form: WorkForm) {
  const attachmentIds = form.attachments.filter((a) => a.id).map((a) => a.id)
  const uploadedVideoUrl = getUploadedVideoUrl(form)
  return {
    title: form.title,
    summary: form.summary,
    techStack: form.techStack,
    advisor: form.advisor,
    coverUrl: form.coverUrl,
    videoUrl: uploadedVideoUrl,
    previewUrl: form.previewUrl,
    runDesc: form.runDescription,
    members: form.members.map((m) => ({
      id: m.id,
      studentId: m.studentId,
      studentName: m.studentName,
      studentNo: m.studentNo,
      className: m.className,
      isLeader: m.isLeader ? 1 : 0,
    })),
    attachmentIds,
  }
}

function getUploadedVideoUrl(form: WorkForm) {
  return form.attachments.find((a) => a.fileType?.toLowerCase?.() === 'mp4')?.fileUrl || ''
}

/* ============ 分页响应适配 ============ */

/* ============ API 函数 ============ */

export function createWork(data: WorkForm) {
  return request({
    url: '/api/student/works',
    method: 'post',
    data: toCreateRequest(data),
  })
}

export function updateWork(id: string | number, data: WorkForm) {
  return request({
    url: `/student/works/${id}`,
    method: 'put',
    data: toUpdateRequest(data),
  })
}

export function deleteWork(id: string | number) {
  return request({
    url: `/student/works/${id}`,
    method: 'delete',
  })
}

export function submitWork(id: string | number) {
  return request({
    url: `/student/works/${id}/submit`,
    method: 'post',
  })
}

export async function getMyWorks(params: WorkListParams) {
  const queryParams: Record<string, string | number> = {
    page: params.page || 1,
    size: params.pageSize || 10,
  }
  const statusNum = toBackendStatus(params.status)
  if (statusNum !== undefined) queryParams.status = statusNum

  const res = await request({
    url: '/api/student/works',
    method: 'get',
    params: queryParams,
  })
  // 适配后端响应：包住 data 中的 PageResult
  res.data = adaptPageResult(res.data, adaptWorkVO)
  return res
}

export async function getWorkDetail(id: string | number) {
  const res = await request({
    url: `/student/works/${id}`,
    method: 'get',
  })
  res.data = adaptWorkVO(res.data)
  return res
}

// ====== 文件上传 ======

export function uploadFile(file: File, workId?: string | number) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/api/file/upload',
    method: 'post',
    data: formData,
    params: {
      workId: workId || undefined,
    },
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
