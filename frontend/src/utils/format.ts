/**
 * 获奖等级 → Element Plus Tag 类型映射
 *
 * 消除 teacher/ranking, public/Ranking, admin/prize 中的重复函数
 */
export function rewardTagType(level: string): string {
  const map: Record<string, string> = {
    一等奖: 'danger',
    二等奖: 'warning',
    三等奖: '',
    优秀奖: 'info',
  }
  return map[level] || ''
}

/**
 * 格式化 ISO 日期时间字符串（将 T 替换为空格）
 *
 * 消除 admin/audit, admin/scoreBatch 等处的内联 replace
 */
export function formatDateTime(value?: string, fallback = ''): string {
  if (!value) return fallback
  return value.replace('T', ' ')
}
