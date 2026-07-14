export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

export interface BackendPageResult<T> {
  records?: T[] | null
  items?: T[] | null
  total?: number
  pageNum?: number
  page?: number
  pageSize?: number
  pageInfo?: {
    page?: number
    pageSize?: number
    total?: number
  }
}

export interface BackendWorkAttachment {
  id?: string | number
  workId?: string | number
  fileName?: string
  fileType?: string
  fileSize?: number
  fileUrl?: string
}

export interface BackendWorkMember {
  id?: string | number
  workId?: string | number
  studentId?: string | number
  studentName?: string
  studentNo?: string
  className?: string
  isLeader?: number | boolean
  avatar?: string
}

export interface BackendWorkVO {
  id: string | number
  title?: string
  summary?: string
  techStack?: string | string[]
  advisor?: string
  coverUrl?: string
  videoUrl?: string
  runDesc?: string
  status: number | string
  statusLabel?: string
  rejectReason?: string
  submitterId: string | number
  submitterName?: string
  submitTime?: string
  submittedAt?: string
  createTime?: string
  updateTime?: string
  attachments?: BackendWorkAttachment[]
  members?: BackendWorkMember[]
  publishStatus: number
  featured?: number
  avgScore?: number | string | null
  rank?: number
  avgInnovation?: string
  avgDifficulty?: string
  avgCompletion?: string
  avgPracticality?: string
  teacherCount?: number
  previewUrl?: string
  likeCount?: number | null
  viewCount?: number | null
  liked?: boolean
  tags?: string[]
}

export const STATUS_MAP: Record<number, 'draft' | 'submitted' | 'rejected' | 'approved'> = {
  0: 'draft',
  1: 'submitted',
  2: 'rejected',
  3: 'approved',
}

export const STATUS_REV_MAP: Record<string, number | undefined> = {
  draft: 0,
  submitted: 1,
  rejected: 2,
  approved: 3,
}

export const PUBLISH_STATUS_MAP: Record<number, 'unpublished' | 'published' | 'offline' | undefined> = {
  0: 'unpublished',
  1: 'published',
  2: 'offline',
}

export function toFrontendStatus(status: number) {
  return STATUS_MAP[status] || 'draft'
}

function normalizeWorkStatus(status: number | string) {
  if (typeof status === 'number') {
    return toFrontendStatus(status)
  }
  const map: Record<string, 'draft' | 'submitted' | 'rejected' | 'approved'> = {
    DRAFT: 'draft',
    SUBMITTED: 'submitted',
    REJECTED: 'rejected',
    APPROVED: 'approved',
  }
  return map[status] || 'draft'
}

export function toBackendStatus(status?: string): number | undefined {
  return status ? STATUS_REV_MAP[status] : undefined
}

export function toFrontendPublishStatus(status: number) {
  return PUBLISH_STATUS_MAP[status]
}

export function adaptAttachment(attachment: BackendWorkAttachment) {
  return {
    id: attachment.id,
    workId: attachment.workId,
    fileName: attachment.fileName || '',
    fileType: attachment.fileType || '',
    fileSize: attachment.fileSize,
    fileUrl: attachment.fileUrl || '',
  }
}

export function adaptMember(member: BackendWorkMember) {
  return {
    id: member.id,
    workId: member.workId,
    studentId: member.studentId,
    studentName: member.studentName || '',
    studentNo: member.studentNo || '',
    className: member.className || '',
    isLeader: member.isLeader === 1 || member.isLeader === true,
    avatar: member.avatar,
  }
}

export function adaptWorkVO(item: BackendWorkVO) {
  return {
    id: item.id,
    title: item.title || '',
    summary: item.summary || '',
    techStack: item.techStack || '',
    advisor: item.advisor || '',
    coverUrl: item.coverUrl || '',
    videoUrl: item.videoUrl || '',
    runDescription: item.runDesc || '',
    status: normalizeWorkStatus(item.status),
    statusLabel: item.statusLabel,
    rejectReason: item.rejectReason,
    submitterId: item.submitterId,
    submitterName: item.submitterName || '',
    submitTime: item.submitTime || item.submittedAt || '',
    createTime: item.createTime || '',
    updateTime: item.updateTime || '',
    attachments: (item.attachments || []).map(adaptAttachment),
    members: (item.members || []).map(adaptMember),
    publishStatus: toFrontendPublishStatus(item.publishStatus),
    featured: item.featured,
    score: item.avgScore != null ? Number(item.avgScore) : undefined,
    rank: item.rank,
    avgInnovation: item.avgInnovation,
    avgDifficulty: item.avgDifficulty,
    avgCompletion: item.avgCompletion,
    avgPracticality: item.avgPracticality,
    teacherCount: item.teacherCount,
    previewUrl: item.previewUrl || '',
    likeCount: item.likeCount ?? 0,
    viewCount: item.viewCount ?? 0,
    liked: item.liked ?? false,
    tags: item.tags || [],
  }
}

export function adaptPageResult<TInput, TOutput>(
  res: BackendPageResult<TInput>,
  adapter: (item: TInput) => TOutput,
  defaultPageSize = 10,
): PageResult<TOutput> {
  const pageInfo = res.pageInfo
  return {
    records: (res.records ?? res.items ?? []).map(adapter),
    total: res.total ?? pageInfo?.total ?? 0,
    page: res.pageNum ?? res.page ?? pageInfo?.page ?? 1,
    pageSize: res.pageSize ?? pageInfo?.pageSize ?? defaultPageSize,
  }
}
