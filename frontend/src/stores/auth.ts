import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  authApi,
  type AuthUser,
  type LoginPayload,
  type RegisterPayload,
} from '../api/auth'
import { apiClient } from '../api/client'

const TOKEN_KEY = 'cityglow-token'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const user = ref<AuthUser | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  // 持久化 token:写入 state、localStorage,并设置 axios 默认 Authorization 头
  function persistToken(newToken: string) {
    token.value = newToken
    localStorage.setItem(TOKEN_KEY, newToken)
    apiClient.defaults.headers.common.Authorization = `Bearer ${newToken}`
  }

  // 清除全部凭证(state + localStorage + axios 头)
  function clearAuth() {
    token.value = null
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    delete apiClient.defaults.headers.common.Authorization
  }

  async function login(payload: LoginPayload) {
    const res = await authApi.login(payload)
    persistToken(res.token)
    user.value = res.user
    return res
  }

  async function register(payload: RegisterPayload) {
    const res = await authApi.register(payload)
    persistToken(res.token)
    user.value = res.user
    return res
  }

  async function fetchMe() {
    if (!token.value) return null
    try {
      const me = await authApi.me()
      user.value = me
      return me
    } catch (e) {
      // token 无效或过期:清除本地凭证
      clearAuth()
      throw e
    }
  }

  function logout() {
    clearAuth()
  }

  return { token, user, isLoggedIn, login, register, logout, fetchMe, clearAuth }
})
