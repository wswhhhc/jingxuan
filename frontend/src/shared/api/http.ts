import type { AxiosRequestConfig } from 'axios'
import request from '@/api/request'

interface CompatibilityEnvelope<T> {
  code: number
  data: T
}

/**
 * 兼容旧接口的 Result 包装与 v1 接口的原始 JSON 响应。
 *
 * Axios 实例会保留旧接口的 `{ code, data }` 响应，Orval 生成的
 * v1 客户端则需要拿到其声明的原始数据类型。
 */
export function unwrapApiResponse<T>(response: unknown): T {
  if (
    typeof response === 'object' &&
    response !== null &&
    'code' in response &&
    'data' in response
  ) {
    return (response as CompatibilityEnvelope<T>).data
  }

  return response as T
}

/**
 * Orval 生成客户端的唯一 HTTP 出口。
 *
 * 当前旧接口仍由响应拦截器解包 Result；迁移到 v2 RFC Problem Details 后，
 * 只需在这一层调整错误映射，无需修改生成代码或页面。
 */
export async function apiRequest<T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig,
): Promise<T> {
  const response = await request({ ...config, ...options })
  return unwrapApiResponse<T>(response)
}
