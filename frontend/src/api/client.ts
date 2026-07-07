import axios from 'axios'

export const apiClient = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

// 统一解包 ApiResponse:返回 data 字段
apiClient.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.code === 200) {
      return body.data
    }
    return Promise.reject(new Error(body?.message || 'API error'))
  },
  (error) => Promise.reject(error),
)

// 通用类型:ApiResponse 解包前
export type ApiResponse<T> = { code: number; message: string; data: T }
