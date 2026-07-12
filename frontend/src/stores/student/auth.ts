import { defineStore } from 'pinia'
import { ref } from 'vue'
import { postAuthLogin, getAuthMe } from '@/shared/api/generated/auth/auth'
import type { V1LoginResponse, V1UserInfo } from '@/shared/api/generated/models'
import { clearAuthStorage } from '@/utils/auth'
import router from '@/router'
import type { AxiosResponse } from 'axios'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(sessionStorage.getItem('token') || localStorage.getItem('token') || '')
  const userInfo = ref<V1UserInfo | null>(null)

  async function login(username: string, password: string, remember = false) {
    const response = await postAuthLogin({
      data: { username, password, rememberMe: remember },
    }) as unknown as AxiosResponse<V1LoginResponse>
    const data = response.data
    token.value = data.accessToken

    localStorage.setItem('token', data.accessToken)
    sessionStorage.setItem('token', data.accessToken)

    if (remember) {
      localStorage.setItem('remember', '1')
    }

    await fetchUserInfo()
  }

  async function fetchUserInfo() {
    const response = await getAuthMe() as unknown as AxiosResponse<V1UserInfo>
    userInfo.value = response.data
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
