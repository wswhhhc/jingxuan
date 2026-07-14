import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ProblemDetails } from '@/shared/api/generated/models/problemDetails'
import { clearAuthStorage as clearSharedAuthStorage, getAuthToken } from '@/utils/auth'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 30000,
})

interface ResponsePayload {
  code?: number
  data?: unknown
  message?: string
}

export class ApiProblemError extends Error implements ProblemDetails {
  readonly type: string
  readonly title: string
  readonly status: number
  readonly detail: string
  readonly instance: string
  readonly code: string
  readonly requestId: string
  readonly fieldErrors: ProblemDetails['fieldErrors']

  constructor(problem: ProblemDetails, message = problem.detail) {
    super(message)
    this.name = 'ApiProblemError'
    this.type = problem.type
    this.title = problem.title
    this.status = problem.status
    this.detail = problem.detail
    this.instance = problem.instance
    this.code = problem.code
    this.requestId = problem.requestId
    this.fieldErrors = problem.fieldErrors
  }
}

export function normalizeResponsePayload(payload: unknown): ResponsePayload {
  if (typeof payload === 'object' && payload !== null && 'code' in payload) {
    return payload as ResponsePayload
  }
  return { code: 0, data: payload }
}

function extractMessage(payload: unknown): string | undefined {
  if (!payload) return undefined
  if (typeof payload === 'string') {
    try {
      const parsed = JSON.parse(payload)
      return extractMessage(parsed) || payload
    } catch {
      return payload
    }
  }
  if (typeof payload === 'object' && payload !== null) {
    const { message, detail } = payload as {
      message?: unknown
      detail?: unknown
    }
    if (typeof message === 'string' && message.trim()) {
      return message
    }
    if (typeof detail === 'string' && detail.trim()) {
      return detail
    }
  }
  return undefined
}

function asProblemDetails(payload: unknown): ProblemDetails | undefined {
  if (typeof payload === 'string') {
    try {
      return asProblemDetails(JSON.parse(payload))
    } catch {
      return undefined
    }
  }
  if (typeof payload !== 'object' || payload === null) {
    return undefined
  }

  const candidate = payload as Record<string, unknown>
  const fieldErrors = candidate.fieldErrors
  if (
    typeof candidate.type !== 'string' ||
    typeof candidate.title !== 'string' ||
    typeof candidate.status !== 'number' ||
    typeof candidate.detail !== 'string' ||
    typeof candidate.instance !== 'string' ||
    typeof candidate.code !== 'string' ||
    typeof candidate.requestId !== 'string' ||
    typeof fieldErrors !== 'object' ||
    fieldErrors === null ||
    Array.isArray(fieldErrors) ||
    !Object.values(fieldErrors).every((value) => typeof value === 'string')
  ) {
    return undefined
  }

  return candidate as unknown as ProblemDetails
}

function getResponseText(request: unknown): unknown {
  if (typeof request !== 'object' || request === null || !('responseText' in request)) {
    return undefined
  }
  return (request as { responseText?: unknown }).responseText
}

function getErrorMessage(error: unknown): string {
  if (!axios.isAxiosError(error)) {
    return error instanceof Error ? error.message : '网络错误'
  }
  return (
    extractMessage(error.response?.data) ||
    extractMessage(getResponseText(error.request)) ||
    extractMessage(getResponseText(error.response?.request)) ||
    error.message ||
    '网络错误'
  )
}

function isLoginRequest(config?: { url?: string | undefined } | null): boolean {
  return config?.url?.includes('/auth/login') ?? false
}

function clearAuthStorage() {
  clearSharedAuthStorage()
  localStorage.removeItem('remember')
}

request.interceptors.request.use(
  (config) => {
    const token = getAuthToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (response) => {
    const res = normalizeResponsePayload(response.data)
    if (res.code === 0 || res.code === 200) {
      return res as unknown as typeof response
    }
    const loginRequest = isLoginRequest(response.config)
    if (!loginRequest) {
      ElMessage.error(res.message || '请求失败')
    }
    if (res.code === 401) {
      clearAuthStorage()
      if (!loginRequest) {
        window.location.href = '/jingxuan/login'
      }
    }
    return Promise.reject(new Error(res.message || (loginRequest ? '账号或密码错误' : '请求失败')))
  },
  (error: unknown) => {
    const axiosError = axios.isAxiosError<unknown>(error) ? error : undefined
    const loginRequest = isLoginRequest(axiosError?.config)
    const responsePayload = axiosError?.response?.data
    const problemDetails = asProblemDetails(responsePayload)
    const payloadCode =
      typeof responsePayload === 'object' &&
      responsePayload !== null &&
      typeof (responsePayload as { code?: unknown }).code === 'number'
        ? (responsePayload as { code: number }).code
        : undefined
    const code = payloadCode ?? axiosError?.response?.status
    const fallbackMsg = loginRequest && code === 401 ? '账号或密码错误' : '网络错误'
    const msg = getErrorMessage(error) || fallbackMsg
    const finalMsg = loginRequest && code === 401 ? '账号或密码错误' : msg
    if (code === 401) {
      clearAuthStorage()
    }
    if (!loginRequest) {
      ElMessage.error(finalMsg)
    }
    return Promise.reject(problemDetails ? new ApiProblemError(problemDetails, finalMsg) : new Error(finalMsg))
  },
)

export default request
