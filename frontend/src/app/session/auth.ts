import { defineStore } from 'pinia'
import { ref } from 'vue'
import { postAuthLogin, getAuthMe } from '@/shared/api/generated/auth/auth'
import type { V1LoginResponse, V1UserInfo } from '@/shared/api/generated/models'
import { clearAuthStorage } from '@/utils/auth'
import router from '@/router'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(sessionStorage.getItem('token') || localStorage.getItem('token') || '')
  const userInfo = ref<V1UserInfo | null>(null)

  async function login(username: string, password: string, remember = false) {
    const response = await postAuthLogin({ username, password, rememberMe: remember })
    const data = response as unknown as V1LoginResponse
    if (!data.accessToken) {
      throw new Error('登录响应缺少 accessToken')
    }
    const accessToken = data.accessToken
    token.value = accessToken

    localStorage.setItem('token', accessToken)
    sessionStorage.setItem('token', accessToken)

    if (remember) {
      localStorage.setItem('remember', '1')
    }

    await fetchUserInfo()
  }

  async function fetchUserInfo() {
    const response = await getAuthMe()
    userInfo.value = response as unknown as V1UserInfo
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    clearAuthStorage()
    router.push('/login')
  }

  return { token, userInfo, login, fetchUserInfo, logout }
})
