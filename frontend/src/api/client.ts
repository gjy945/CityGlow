import axios from 'axios'
import i18n from '../i18n'

export const apiClient = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

const TOKEN_KEY = 'cityglow-token'

// 请求拦截器:自动附加 Authorization 与 Accept-Language
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  config.headers.set('Accept-Language', i18n.global.locale.value)
  return config
})

// 响应拦截器:统一解包 ApiResponse(code === 200 时返回 data);
// 401 时清除本地凭证并跳转登录页(带 redirect 回跳地址)
apiClient.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.code === 200) {
      return body.data
    }
    return Promise.reject(new Error(body?.message || 'API error'))
  },
  (error) => {
    if (error?.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      delete apiClient.defaults.headers.common.Authorization
      // /auth/me 探测失败仅清凭证,避免公共页被强制跳转登录;
      // 其余受保护资源 401 才跳转登录页(带 redirect 回跳地址)
      const url = error.config?.url ?? ''
      const isSessionProbe = url.includes('/auth/me')
      if (!isSessionProbe) {
        const { pathname, search } = window.location
        if (!pathname.startsWith('/login')) {
          const redirect = encodeURIComponent(pathname + search)
          window.location.href = `/login?redirect=${redirect}`
        }
      }
    }
    return Promise.reject(error)
  },
)

// 通用类型:ApiResponse 解包前
export type ApiResponse<T> = { code: number; message: string; data: T }
