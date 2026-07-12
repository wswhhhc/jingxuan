import type { V1WorkSummary } from '../generated/models/v1WorkSummary'
import type { V1WorkDetail } from '../generated/models/v1WorkDetail'
import type { V1Tag } from '../generated/models/v1Tag'
import type { V1Batch } from '../generated/models/v1Batch'
import type { V1Task } from '../generated/models/v1Task'

/* ====== Public WorkList adapter ====== */
export interface AdaptedWorkListItem {
  id: number | string
  title: string
  summary: string
  techStack: string
  submitterName: string
  submitTime: string
  featured: number
  coverUrl: string
  tags: string[]
  likeCount: number
  viewCount: number
  previewUrl: string
  status: string
  batchId?: string
}

export function adaptWorkSummary(item: V1WorkSummary): AdaptedWorkListItem {
  return {
    id: item.id ?? '',
    title: item.title ?? '',
    summary: item.summary ?? '',
    techStack: (item as any).techStack ?? ((item.tags?.join(', ')) ?? ''),
    submitterName: item.submitterName ?? '',
    submitTime: item.submittedAt ?? '',
    featured: (item as any).featured ?? 0,
    coverUrl: (item as any).coverUrl ?? '',
    tags: item.tags ?? [],
    likeCount: (item as any).likeCount ?? 0,
    viewCount: (item as any).viewCount ?? 0,
    previewUrl: (item as any).previewUrl ?? '',
    status: item.status ?? '',
    batchId: item.batchId,
  }
}

/* ====== Public WorkDetail adapter ====== */
export interface AdaptedWorkDetail {
  id: number | string
  title: string
  summary: string
  techStack: string
  advisor: string
  coverUrl: string
  videoUrl: string
  runDescription: string
  submitterId: number | string
  submitterName: string
  submitTime: string
  previewUrl: string
  tags: string[]
  members: AdaptedMember[]
  attachments: AdaptedAttachment[]
  score?: number
  rank?: number
  scoreDetail?: {
    avgInnovation?: string
    avgDifficulty?: string
    avgCompletion?: string
    avgPracticality?: string
  }
  likeCount: number
  viewCount: number
  liked: boolean
  featured: boolean
}

export interface AdaptedMember {
  id?: string | number
  studentId?: string | number
  studentName: string
  studentNo: string
  className: string
  isLeader: boolean
  avatar?: string
}

export interface AdaptedAttachment {
  id?: string | number
  fileName: string
  fileType: string
  fileUrl: string
  fileSize?: number
}

export function adaptWorkDetail(item: V1WorkDetail): AdaptedWorkDetail {
  return {
    id: item.id ?? '',
    title: item.title ?? '',
    summary: item.summary ?? '',
    techStack: item.techStack ?? (item.tags?.join(', ') ?? ''),
    advisor: item.advisor ?? '',
    coverUrl: item.coverUrl ?? '',
    videoUrl: item.videoUrl ?? '',
    runDescription: item.runDescription ?? '',
    submitterId: item.submitterId ?? '',
    submitterName: item.submitterName ?? '',
    submitTime: item.submittedAt ?? '',
    previewUrl: item.previewUrl ?? '',
    tags: item.tags ?? [],
    members: (item.members ?? []).map((m) => ({
      id: m.id,
      studentId: m.studentId,
      studentName: m.name ?? '',
      studentNo: m.studentNumber ?? '',
      className: m.className ?? '',
      isLeader: m.leader ?? false,
      avatar: m.avatarUrl,
    })),
    attachments: (item.attachments ?? []).map((a) => ({
      id: a.id,
      fileName: a.fileName ?? '',
      fileType: a.contentType ?? '',
      fileUrl: a.url ?? '',
      fileSize: a.size,
    })),
    score: item.averageScore != null ? Number(item.averageScore) : undefined,
    rank: item.rank,
    likeCount: item.likeCount ?? 0,
    viewCount: item.viewCount ?? 0,
    liked: item.liked ?? false,
    featured: item.featured ?? false,
  }
}

/* ====== Class / Tag adapter ====== */
export interface AdaptedClassItem {
  id: number | string
  dictLabel: string
}

export function adaptClassItem(item: any): AdaptedClassItem {
  return {
    id: item.id ?? item.value ?? '',
    dictLabel: item.label ?? item.name ?? '',
  }
}

export function adaptTagItem(item: V1Tag) {
  return {
    id: item.id ?? '',
    dictLabel: item.name ?? '',
  }
}

/* ====== Batch adapter ====== */
export function adaptBatch(item: V1Batch) {
  return {
    batchId: item.id,
    batchName: item.name,
    startTime: item.startAt,
    endTime: item.endAt,
    status: item.status === 'ACTIVE' ? 1 : 0,
  }
}

/* ====== Task adapter ====== */
export interface AdaptedTask {
  id: number
  batchId: number
  workId: number | null
  title: string
  content: string
  status: number
  createTime: string
  batchName: string
  startTime?: string
  endTime?: string
}

export function adaptTask(item: V1Task): AdaptedTask {
  let statusNum = 0
  const s = item.status?.toLowerCase()
  if (s === 'completed' || s === 'done') statusNum = 1
  else if (s === 'rejected') statusNum = 2
  else if (s === 'expired') statusNum = 3

  return {
    id: Number(item.id ?? 0),
    batchId: Number(item.batchId ?? 0),
    workId: item.workId ? Number(item.workId) : null,
    title: item.title ?? '',
    content: item.content ?? '',
    status: statusNum,
    createTime: '',
    batchName: item.batchName ?? '',
    startTime: '',
    endTime: item.endAt,
  }
}
