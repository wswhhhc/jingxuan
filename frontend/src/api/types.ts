/* 与后端 DTO 对齐的共享类型 */

/** 作品列表 VO — WorkListVO */
export interface WorkListVO {
  id: number
  title: string
  techStack: string
  coverUrl: string
  previewUrl: string
  status: number // 0=草稿 1=已提交 2=已驳回 3=已通过
  statusLabel: string
  submitterName: string
  submitTime: string
  publishStatus: number // 0=未发布 1=已发布 2=已下线
  featured: number
  memberCount: number
  batchId?: number
  batchName?: string
  scored?: boolean
}

/** 作品详情 VO — WorkDetailVO */
export interface WorkDetailVO {
  id: number
  title: string
  summary: string
  techStack: string
  advisor: string
  coverUrl: string
  videoUrl: string
  runDesc: string
  status: number
  statusLabel: string
  submitterId: number
  submitterName: string
  submitTime: string
  batchId: number
  publishStatus: number
  publishStatusLabel: string
  featured: number
  previewUrl: string
  avgScore: string
  members: {
    id: number
    studentId: number
    studentName: string
    studentNo: string
    className: string
    isLeader: number
  }[]
  attachments: {
    id: number
    fileName: string
    fileType: string
    fileUrl: string
  }[]
}

/** 共享常量 */
export const IMAGE_TYPES: readonly string[] = ['jpg', 'jpeg', 'png', 'gif', 'webp']
export const VIDEO_TYPES: readonly string[] = ['mp4', 'avi', 'mov', 'mkv', 'webm']
export const MEDIA_TYPES: readonly string[] = [...IMAGE_TYPES, ...VIDEO_TYPES]

/** 排行项 — 公开端/教师端共用 */
export interface RankItem {
  rankNo: number
  workId: number
  workTitle: string
  techStack: string
  coverUrl: string
  advisor?: string
  avgScore: number
  avgInnovation: number
  avgDifficulty: number
  avgCompletion: number
  avgPracticality: number
  teacherCount: number
  submitTime: string
  /** 获奖等级文案，如「一等奖」 */
  rewardLevel: string
  /** 兼容字段，与 rewardLevel 相同 */
  rewardName: string
  /** 奖品说明，如「荣誉证书 + 500元京东卡」 */
  prizeName: string
}

/** 键值选项（用于下拉筛选等场景） */
export interface CategoryItem {
  label: string
  value: string
}

/** 用户信息（登录后缓存，所有角色通用） */
export interface UserInfo {
  id: number
  username: string
  realName: string
  roleId: number
  roleCode: string
  roleName: string
  className: string
  classId?: number
  avatar?: string
  email?: string
  phone?: string
  firstLogin?: boolean
}
