import type { AxiosRequestConfig } from 'axios'
import request from '@/api/request'

/**
 * Orval 生成客户端的唯一 HTTP 出口。
 *
 * 当前旧接口仍由响应拦截器解包 Result；迁移到 v2 RFC Problem Details 后，
 * 只需在这一层调整错误映射，无需修改生成代码或页面。
 */
export function apiRequest<T>(config: AxiosRequestConfig, options?: AxiosRequestConfig): Promise<T> {
  return request({ ...config, ...options }) as unknown as Promise<T>
}
